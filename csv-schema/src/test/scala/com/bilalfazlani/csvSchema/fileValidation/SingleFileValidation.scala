package com.bilalfazlani.csvSchema
package fileValidation

import zio.NonEmptyChunk
import zio.ZIO
import zio.durationInt
import zio.nio.charset.Charset
import zio.nio.file.Files
import zio.nio.file.Path
import zio.prelude.NonEmptySet
import zio.stream.ZPipeline
import zio.stream.ZSink
import zio.test.Assertion.*
import zio.test.TestAspect.timeout
import zio.test.*

object SingleFileValidation extends ZIOSpecDefault {

  private def pathOf(str: String) =
    Path("./csv-schema/src/test/resources/singleFileValidation") / str

  private val schema = ColumnSchema.StringSchema(
    columnName = "name",
    maxLength = Some(100),
    minLength = Some(3),
    regex = Some("[a-zA-Z]*".r)
  ) &
    ColumnSchema.StringSchema(
      columnName = "city",
      allowedValues = Set(
        "Mumbai",
        "Pune",
        "Delhi"
      )
    ) &
    ColumnSchema.BooleanSchema(
      columnName = "selfEmployed"
    ) &
    ColumnSchema.IntegerSchema(
      columnName = "age",
      min = Some(10),
      max = Some(100),
      required = false
    ) &
    ColumnSchema.DoubleSchema(
      columnName = "salary",
      min = Some(1000.0002),
      max = Some(8000.999),
      required = false
    )

  def spec =
    suite("Single file validation")(
      test("successful validation") {
        assertZIO(
          CsvValidationLive.validate(
            schema,
            pathOf("successful.csv")
          )
        )(isUnit)
      },
      test("empty file") {
        val path = pathOf("missingHeaders1.csv")
        val expectedError =
          CsvFailure.SyntaxValidationError(path, 1L, "file is empty")
        val effect = for {
          validation <- CsvValidationLive.validate(
            schema,
            path
          )
        } yield validation

        assertZIO(effect.flip)(equalTo(expectedError))
      },
      test("empty header line") {
        val path = pathOf("missingHeaders2.csv")
        val expectedError =
          CsvFailure.SyntaxValidationError(path, 1L, "header line is blank")
        val effect = for {
          validation <- CsvValidationLive.validate(
            schema,
            path
          )
        } yield validation

        assertZIO(effect.flip)(equalTo(expectedError))
      },
      test("header mismatch") {
        val path = pathOf("differentNumberOfHeaders.csv")
        val effect = for {
          validation <- CsvValidationLive.validate(
            schema,
            path
          )
        } yield validation

        def syntaxErr(str: String) =
          CsvFailure.SyntaxValidationError(path, 1, str)

        assertZIO(effect.flip)(
          isSubtype[CsvFailure.Multiple](
            hasField(
              "errors",
              _.errors,
              hasSameElements(
                NonEmptyChunk(
                  syntaxErr(
                    "2 value(s) found in header. expected number of values: 5"
                  ),
                  syntaxErr(
                    "expected header: 'name', found: 'city'"
                  ),
                  syntaxErr(
                    "expected header: 'city', found: ' country'"
                  )
                )
              )
            )
          )
        )
      }
    ) @@ timeout(
      5.seconds
    )
}
