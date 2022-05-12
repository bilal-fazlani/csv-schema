package com.bilalfazlani.csvSchema

import zio.{ZIO, IO, UIO}
import zio.nio.file.Path
import zio.nio.file.Files
import zio.nio.charset.Charset
import java.io.IOException
import zio.stream.ZStream
import zio.prelude.Validation

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
      header: Header,
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
        if (length != header.value.length)(
          Left(
            CsvFailure.SyntaxValidationError(
              path,
              lineNumber,
              s"${length} value(s) found. expected number of values: ${header.value.length}"
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

  private def validateSinglePath(
      path: Path,
      header: Header,
      schema: CsvSchema
  ): IO[CsvFailure, Unit] = {
    Files
      .lines(path)
      .mapError(e => CsvFailure.ReadingError(path, e.getMessage, e))
      .zipWithIndex
      .map { case (line, index) =>
        if index != 0 then validateLine(path, header, line, index + 1, schema)
        else Right(())
      }
      .collect { case Left(e) => e }
      .runCollect
      .map(_.reverse)
      .foldZIO(
        ZIO.fail,
        errors =>
          if errors.isEmpty then ZIO.succeed(())
          else ZIO.fail(errors.reduce(_ + _))
      )
  }

  def validateFile(path: Path, schema: CsvSchema): IO[CsvFailure, List[Path]] =
    for {
      header <- csvHeaderOf(path)
      _ <- validateSinglePath(path, header, schema)
    } yield List(path)

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
          .validatePar(allCsvPaths)(p =>
            validateSinglePath(p, firstHeader, schema)
          )
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
