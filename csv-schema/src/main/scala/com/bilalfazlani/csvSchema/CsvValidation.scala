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
import zio.ZLayer

trait CsvValidation {
  def validate(
      schema: CsvSchema,
      path: Path,
      paths: Path*
  ): ZIO[Scope, CsvFailure, Unit]
}

object CsvValidation {
  val live = ZLayer.succeed(CsvValidationLive)

  def validate(schema: CsvSchema, path: Path, paths: Path*) =
    ZIO.serviceWithZIO[CsvValidation](
      _.validate(
        schema,
        path,
        paths*
      )
    )
}

object CsvValidationLive extends CsvValidation {

  private def validateHeader(
      path: Path,
      line: String,
      lineNumber: Long,
      schema: CsvSchema
  ): Either[CsvFailure, Unit] =
    val values = line.split(",", -1)
    val length = values.length
    val v = for {
      _ <-
        if (line.isBlank)(
          Validation.fail(
            CsvFailure
              .SyntaxValidationError(path, lineNumber, s"header line is blank")
          )
        )
        else Validation.unit
      result2 <-
        val sizeValidation = if (length != schema.columns.length)(
          Validation.fail(
            CsvFailure.SyntaxValidationError(
              path,
              lineNumber,
              s"${length} value(s) found in header. expected number of values: ${schema.columns.length}"
            )
          )
        )
        else Validation.unit
        val columnValidation = Validation
          .validateAll(
            schema.columns
              .zip(values)
              .map {
                case (column, value) if column.columnName.trim == value.trim =>
                  Validation.unit
                case (column, value) =>
                  Validation.fail(
                    CsvFailure.SyntaxValidationError(
                      path,
                      lineNumber,
                      s"expected header: '${column.columnName}', found: '$value'"
                    )
                  )
              }
          )
          .map(_ => ())

        Validation.validate(sizeValidation, columnValidation).map(_ => ())
    } yield result2

    v.toEither.left.map(_.reduce(_ + _))

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
  ): ZIO[Scope, CsvFailure, Unit] =
    val stream = source
      .via(ZPipeline.utfDecode)
      .via(ZPipeline.splitLines)
      .mapError(e => CsvFailure.CsvReadingError(path, e))
      .via(zipWithLineNumber)
      .map {
        case (line, 1) =>
          validateHeader(path, line, 1, schema)
        case (line, lineNumber) =>
          validateLine(path, line, lineNumber, schema)
      }

    stream
      .broadcast(2, 1024)
      .flatMap { streams =>
        val stream1 = streams(0)
        val stream2 = streams(1)
        for {
          validation <- stream1.collectLeft.runCollect
            .map(_.reverse)
            .foldZIO(
              ZIO.fail,
              errors =>
                if errors.isEmpty then ZIO.succeed(())
                else ZIO.fail(errors.reduce(_ + _))
            )
            .fork
          countRef <- stream2.runCount.fork
          count <- validation.join &> countRef.join
          _ <- ZIO.when(count == 0)(
            ZIO
              .fail(CsvFailure.SyntaxValidationError(path, 1L, "file is empty"))
          )
        } yield ()
      }

  def validate(
      schema: CsvSchema,
      path: Path,
      paths: Path*
  ): ZIO[Scope, CsvFailure, Unit] =
    validateAllFiles(schema, NonEmptyChunk(path))

  private def validateAllFiles(
      schema: CsvSchema,
      paths: NonEmptyChunk[Path]
  ): ZIO[Scope, CsvFailure, Unit] =
    ZIO
      .validatePar(paths)(p =>
        validateStream(ZStream.fromFile(p.toFile, 1024))(p, schema)
      )
      .mapError(_.reduce(_ + _))
      .unit
}
