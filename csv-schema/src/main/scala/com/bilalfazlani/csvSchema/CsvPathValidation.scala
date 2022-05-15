package com.bilalfazlani.csvSchema

import zio.{ZIO, IO, UIO}
import zio.nio.file.Path
import zio.nio.file.Files
import zio.nio.charset.Charset
import java.io.IOException
import zio.stream.{ZStream, ZPipeline}
import zio.prelude.Validation
import zio.stream.ZSink
import zio.Scope

object CsvPathValidation {

  case class Header(value: List[String])

  private def csvHeaderOf(path: Path): IO[CsvFailure, Header] = for {
    header <- Files
      .lines(path, Charset.defaultCharset)
      .runHead
      .mapError(e => CsvFailure.ReadingError(path, e.getMessage, e))
      .collect(
        CsvFailure
          .SyntaxValidationError(
            path,
            1,
            "csv file does not contain header line"
          )
      ) {
        case Some(headerLine) if headerLine.trim.nonEmpty =>
          headerLine
      }
    headerValues = header.split(",", -1).map(_.trim).toList
    _ <- ZIO.when(headerValues.exists(_.isEmpty))(
      ZIO.fail(
        CsvFailure.SyntaxValidationError(
          path,
          1,
          "csv headers contains an empty entry"
        )
      )
    )
  } yield Header(headerValues)

  private def validateLine(
      path: Path,
      line: String,
      lineNumber: Long,
      schema: CsvSchema
  ): Either[CsvFailure, Unit] =
    val values = line.split(",", -1)
    val length = values.length
    for {
      result1 <-
        if (line.isBlank)(
          Left(
            CsvFailure
              .SyntaxValidationError(path, lineNumber, s"blank line")
          )
        )
        else Right(())
      result2 <-
        if (length != schema.columns.length)(
          Left(
            CsvFailure.SyntaxValidationError(
              path,
              lineNumber,
              s"${length} value(s) found. expected number of values: ${schema.columns.length}"
            )
          )
        )
        else Right(())
      result3 <- Validation
        .validateAll(schema.columns.zip(values).map(Validator.validate))
        .toEither
        .map(_ => ())
        .left
        .map(errors =>
          CsvFailure
            .SchemaValidationError(path, lineNumber, errors.toSeq)
        )
    } yield ()

  def zipWithLineNumber[A] =
    ZPipeline
      .mapAccum[A, Long, (A, Long)](1L)((index, a) => (index + 1, (a, index)))

  def validateStream(
      source: ZStream[Any, CsvFailure, String]
  )(
      path: Path,
      schema: CsvSchema
  ) =
    val sink = (zipWithLineNumber[String] // add line numbers
      .andThen(ZPipeline.drop(1)) // drop header
      .andThen( // validate lines
        ZPipeline.map[(String, Long), Either[CsvFailure, Unit]](
          (line, lineNumber) => validateLine(path, line, lineNumber, schema)
        )
      )
      // take errors only
      .andThen(ZPipeline.collect { case Left(e) =>
        e
      }) >>> ZSink.collectAll)
      .map(_.reverse)

    (source >>> sink).foldZIO(
      ZIO.fail,
      errors =>
        if errors.isEmpty then ZIO.succeed(())
        else ZIO.fail(errors.reduce(_ + _))
    )

  def validateFileWithSink[R, L, Z](
      path: Path,
      schema: CsvSchema,
      sink: ZSink[R, CsvFailure, String, L, Z]
  ): ZIO[Scope & R, CsvFailure, Z] = {
    Files
      .lines(path)
      .mapError(e => CsvFailure.ReadingError(path, e.getMessage, e))
      .broadcast(2, 2)
      .flatMap { streams =>
        val validationStream = streams(0)
        val customStream = streams(1)
        for {
          validationRef <- validateStream(validationStream)(path, schema).fork
          resultRef <- (customStream >>> sink).fork
          result <- validationRef.join *> resultRef.join
        } yield result
      }
  }

  def validateFile(
      path: Path,
      schema: CsvSchema
  ): IO[CsvFailure, Unit] = {
    val stream = Files
      .lines(path)
      .mapError(e => CsvFailure.ReadingError(path, e.getMessage, e))

    validateStream(stream)(path, schema)
  }

  def validateDirectory(
      path: Path,
      schema: CsvSchema
  ): IO[CsvFailure, List[Path]] = {
    for {
      allCsvPaths <- findCsvFiles(path)
      firstFile <- ZIO
        .fromOption(allCsvPaths.headOption)
        .mapError(_ => IOException(s"no csv files found in $path"))
        .mapError(e => CsvFailure.ReadingError(path, e.getMessage, e))
      firstHeader <- csvHeaderOf(firstFile)
      remainingHeaders <-
        ZIO.collectAllPar(allCsvPaths.drop(1).map(csvHeaderOf))
      headerValidation <- ZIO.collectAllPar(
        remainingHeaders
          .zip(allCsvPaths.drop(1))
          .map {
            case (h @ Header(values), _) if values == firstHeader.value =>
              ZIO.succeed(h)
            case (Header(values), p) =>
              ZIO.fail(
                CsvFailure.SyntaxValidationError(
                  p,
                  1,
                  s"csv headers don't match with $firstFile"
                )
              )
          }
      )
      allCsvFilesValidation <-
        ZIO
          .validatePar(allCsvPaths)(p => validateFile(p, schema))
          .unit
          .mapError(_.reduce(_ + _))
    } yield allCsvPaths
  }

  private def findCsvFiles(directory: Path) = Files
    .find(directory, 10, Set.empty)((p, attr) =>
      p.filename.toString.toLowerCase.endsWith(".csv") && p.toFile.isFile
    )
    .runCollect
    .map(_.toList)
    .mapError(e => CsvFailure.ReadingError(directory, e.getMessage, e))
}
