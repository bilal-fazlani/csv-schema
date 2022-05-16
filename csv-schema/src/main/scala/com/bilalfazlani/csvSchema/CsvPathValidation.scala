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
import zio.NonEmptyChunk
import zio.config.magnolia.examples.P

trait CsvPathValidation {
  def validateFiles(
      schema: CsvSchema,
      paths: NonEmptyChunk[Path]
  ): IO[CsvFailure, Unit]

  def validateFile(
      schema: CsvSchema,
      path: Path
  ): IO[CsvFailure, Unit]

  def validateFileAndAggregate[R, Z](
      schema: CsvSchema,
      path: Path
  )(
      sink: ZSink[R, Throwable, Byte, ?, Z]
  ): ZIO[Scope & R, CsvFailure, Z]

  def validateFilesAndAggregate[R, Z](
      schema: CsvSchema,
      paths: NonEmptyChunk[Path]
  )(
      sink: ZSink[R, Throwable, Byte, ?, Z]
  ): ZIO[Scope & R, CsvFailure, Map[Path, Z]]
}

object CsvPathValidation extends CsvPathValidation {

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

  private def zipWithLineNumber[A]: ZPipeline[Any, Nothing, A, (A, Long)] =
    ZPipeline
      .mapAccum[A, Long, (A, Long)](1L)((index, a) => (index + 1, (a, index)))

  private def validateStream(
      source: ZStream[Any, Throwable, Byte]
  )(
      path: Path,
      schema: CsvSchema
  ): IO[CsvFailure, Unit] =
    source
      .via(ZPipeline.utfDecode)
      .via(ZPipeline.splitLines)
      .mapError(e => CsvFailure.ReadingError(path, e.getMessage, e))
      .via(zipWithLineNumber)
      .drop(1) // drop header line
      .map((line, lineNumber) => validateLine(path, line, lineNumber, schema))
      .collectLeft
      .runCollect
      .map(_.reverse)
      .foldZIO(
        ZIO.fail,
        errors =>
          if errors.isEmpty then ZIO.succeed(())
          else ZIO.fail(errors.reduce(_ + _))
      )

  private def validateFileWithAggregation[R, L, Z](
      source: ZStream[Any, Throwable, Byte],
      path: Path,
      schema: CsvSchema,
      aggregateSink: ZSink[R, Throwable, Byte, L, Z]
  ): ZIO[Scope & R, CsvFailure, Z] = {
    source
      .broadcast(2, 100)
      .flatMap { streams =>
        val validationStream = streams(0)
        val aggregateStream = streams(1)

        for {
          validationRef <- validateStream(validationStream)(path, schema).fork
          resultRef <- (aggregateStream >>> aggregateSink)
            .mapError(CsvFailure.ProcessingError(path, _))
            .fork
          result <- validationRef.join *> resultRef.join
        } yield result
      }
  }

  def validateFile(
      schema: CsvSchema,
      path: Path
  ): IO[CsvFailure, Unit] = validateFiles(schema, NonEmptyChunk(path))

  def validateFiles(
      schema: CsvSchema,
      paths: NonEmptyChunk[Path]
  ): IO[CsvFailure, Unit] =
    for {
      _ <- validateCsvHeaders(paths)
      _ <-
        ZIO
          .validatePar(paths)(p =>
            validateStream(ZStream.fromFile(p.toFile, 1024))(p, schema)
          )
          .mapError(_.reduce(_ + _))
    } yield ()

  def validateFileAndAggregate[R, Z](
      schema: CsvSchema,
      path: Path
  )(
      aggregateSink: ZSink[R, Throwable, Byte, ?, Z]
  ): ZIO[Scope & R, CsvFailure, Z] =
    for {
      _ <- validateCsvHeaders(NonEmptyChunk(path))
      result <- validateFileWithAggregation(
        ZStream.fromFile(path.toFile, 1024),
        path,
        schema,
        aggregateSink
      )
    } yield result

  def validateFilesAndAggregate[R, Z](
      schema: CsvSchema,
      paths: NonEmptyChunk[Path]
  )(
      fileSink: ZSink[R, Throwable, Byte, ?, Z]
  ): ZIO[Scope & R, CsvFailure, Map[Path, Z]] =
    for {
      _ <- validateCsvHeaders(paths)
      chunks <-
        ZIO
          .validatePar(paths)(p =>
            validateFileWithAggregation(
              ZStream.fromFile(p.toFile, 1024),
              p,
              schema,
              fileSink
            ).map((p, _))
          )
          .mapError(_.reduce(_ + _))
    } yield chunks.toMap

  private def validateCsvHeaders(
      allCsvPaths: NonEmptyChunk[Path]
  ): IO[CsvFailure, NonEmptyChunk[Path]] =
    val firstFile = allCsvPaths.head
    for {
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
    } yield allCsvPaths
}
