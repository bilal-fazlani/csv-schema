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

object SingleFileValidation extends ZIOSpecDefault {

  private def pathOf(path: String) =
    (Path("./csv-schema/src/test/resources") / path)

  private def pathOf(path: Path) =
    (Path("./csv-schema/src/test/resources") / path)

  private val schemaZ = CsvSchema(pathOf("schema.yml"))

  def spec = suite("Single file validation without aggregation")(
    test("successful validation") {
      for {
        schema <- schemaZ
        path = pathOf(
          Path("singleFileValidation") / "noAggregation" / "successful.csv"
        )
        validation <- CsvPathValidation.validateFile(
          schema,
          path
        )
      } yield assert(validation)(isUnit)
    },
    test("missing headers - empty file") {
      val path = pathOf(
        Path(
          "singleFileValidation"
        ) / "noAggregation" / "missingHeaders1.csv"
      )
      val expectedError =
        CsvFailure.SyntaxValidationError(path, 1L, "file is empty")
      val effect = for {
        schema <- schemaZ
        validation <- CsvPathValidation.validateFile(
          schema,
          path
        )
      } yield validation

      assertZIO(effect.flip)(equalTo(expectedError))
    },
    test("missing headers - empty header line") {
      val path = pathOf(
        Path("singleFileValidation") / "noAggregation" / "missingHeaders2.csv"
      )
      val expectedError =
        CsvFailure.SyntaxValidationError(path, 1L, "header line is blank")
      val effect = for {
        schema <- schemaZ
        validation <- CsvPathValidation.validateFile(
          schema,
          path
        )
      } yield validation

      assertZIO(effect.flip)(equalTo(expectedError))
    },
    test("different headers") {
      val path = pathOf(
        Path(
          "singleFileValidation"
        ) / "noAggregation" / "dfferentNumberOfHeaders.csv"
      )
      val expectedError =
        CsvFailure.SyntaxValidationError(path, 0L, "-----")
      val effect = for {
        schema <- schemaZ
        validation <- CsvPathValidation.validateFile(
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
  ) @@ timeout(5.seconds)
}
