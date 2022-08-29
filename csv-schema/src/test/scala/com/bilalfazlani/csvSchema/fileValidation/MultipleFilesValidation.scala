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

object MultipleFilesValidation extends ZIOSpecDefault {

  private def pathOf(str: String) =
    Path("./csv-schema/src/test/resources") / str

  private def pathOf(path: Path) =
    Path("./csv-schema/src/test/resources") / path

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
    suite("Multiple files validation")(
      test("successful validation") {
        assertZIO(
          CsvValidationLive.validate(
            schema,
            pathOf("multipleValidCsvFiles") / "successful.csv",
            pathOf("multipleValidCsvFiles") / "successful-2.csv",
            pathOf("multipleValidCsvFiles") / "successful-3.csv"
          )
        )(isUnit)
      },
      test("validation with errors"){
        val path1 = pathOf(Path("multipleCsvFilesWithErrors") / "missingHeaders1.csv")
        val path2 = pathOf(Path("multipleCsvFilesWithErrors") / "missingHeaders2.csv")
        val path3 = pathOf(Path("multipleCsvFilesWithErrors") / "differentNumberOfHeaders.csv")
        val path4 = pathOf(Path("multipleCsvFilesWithErrors") / "successful.csv")

        def syntaxErr(path: Path)(str: String) =
          CsvFailure.SyntaxValidationError(path, 1, str)

        val effect = for {
          validation <- CsvValidationLive.validate(
            schema,
            path1, path2, path3, path4
          )
        } yield validation

        assertZIO(effect.flip)(
          isSubtype[CsvFailure.Multiple](
            hasField(
              "errors",
              _.errors,
              hasSameElements(
                NonEmptyChunk(
                  CsvFailure.SyntaxValidationError(path1, 1L, "file is empty"),
                  CsvFailure.SyntaxValidationError(path2, 1L, "header line is blank"),
                  syntaxErr(path3)(
                    "2 value(s) found in header. expected number of values: 5"
                  ),
                  syntaxErr(path3)(
                    "expected header: 'name', found: 'city'"
                  ),
                  syntaxErr(path3)(
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
