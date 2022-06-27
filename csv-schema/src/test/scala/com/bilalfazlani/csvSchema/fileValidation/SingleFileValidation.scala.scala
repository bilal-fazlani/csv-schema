package com.bilalfazlani.csvSchema
package fileValidation

import zio.test.*
import zio.test.Assertion.*
import zio.nio.file.Path
import zio.nio.file.Files
import zio.nio.charset.Charset
import zio.ZIO
import zio.test.TestAspect.timeout
import zio.durationInt
import zio.prelude.NonEmptySet
import zio.NonEmptyChunk
import zio.stream.ZSink
import zio.stream.ZPipeline
import com.roundeights.hasher.Foldable
import com.roundeights.hasher.Algo

object SingleFileValidation extends ZIOSpecDefault {

  private def pathOf(path: String) =
    (Path("./csv-schema/src/test/resources") / path)

  private def pathOf(path: Path) =
    (Path("./csv-schema/src/test/resources") / path)

  private val schema = CsvSchema(columns =
    List(
      ColumnSchema.StringSchema(
        columnName = "name",
        maxLength = Some(100),
        minLength = Some(3),
        regex = Some("[a-zA-Z]*".r)
      ),
      ColumnSchema.StringSchema(
        columnName = "city",
        allowedValues = Set(
          "Mumbai",
          "Pune",
          "Delhi"
        )
      ),
      ColumnSchema.BooleanSchema(
        columnName = "selfEmployed"
      ),
      ColumnSchema.IntegerSchema(
        columnName = "age",
        min = Some(10),
        max = Some(100),
        required = false
      )
    )
  )

  def spec =
    suite("Single file validation")(
      test("successful validation") {
        assertZIO(
          CsvValidationLive.validate(
            schema,
            pathOf(
              Path("singleFileValidation") / "noAggregation" / "successful.csv"
            )
          )
        )(isUnit)
      },
      test("empty file") {
        val path = pathOf(
          Path(
            "singleFileValidation"
          ) / "noAggregation" / "missingHeaders1.csv"
        )
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
        val path = pathOf(
          Path("singleFileValidation") / "noAggregation" / "missingHeaders2.csv"
        )
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
        val path = pathOf(
          Path(
            "singleFileValidation"
          ) / "noAggregation" / "dfferentNumberOfHeaders.csv"
        )
        val expectedError =
          CsvFailure.SyntaxValidationError(path, 0L, "-----")
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
                    "2 value(s) found in header. expected number of values: 4"
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
