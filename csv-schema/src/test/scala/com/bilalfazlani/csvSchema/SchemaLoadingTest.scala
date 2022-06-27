package com.bilalfazlani.csvSchema

import zio.test.*
import zio.test.Assertion.*
import zio.nio.file.Path
import zio.test.TestConstructor.WithOut
import zio.ZIO

object SchemaLoadingTest extends ZIOSpecDefault {
  private def pathOf(path: String) =
    (Path("./csv-schema/src/test/resources") / path)

  private def invalidTest(testName: String, fileName: String) = test(testName) {
    val schema = CsvSchema.File(pathOf(fileName)).load
    assertZIO(schema.exit)(
      fails(
        isSubtype[CsvFailure.SchemaParsingError](
          assertion("path comparision")(_.file == pathOf(fileName))
        )
      )
    )
  }

  val spec = suite("SchemaLoadingTest")(
    test("load schema successfully") {
      val expectedSchema = CsvSchema.Inline(columns =
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
      assertZIO(CsvSchema.File(pathOf("schema.yml")).load)(equalTo(expectedSchema))
    },
    test("return error when file does not exist") {
      assertZIO(CsvSchema.File(pathOf("no-schema.yml")).load.exit)(
        fails(
          equalTo(
            CsvFailure.SchemaFileNotFound(pathOf("no-schema.yml"))
          )
        )
      )
    },
    invalidTest(
      "return error data types are incorrect",
      "invalid-schema-1.yaml"
    ),
    invalidTest("return error fields are incorrect", "invalid-schema-2.yaml"),
    invalidTest("return error format incorrect", "invalid-yaml.yaml")
  )
}
