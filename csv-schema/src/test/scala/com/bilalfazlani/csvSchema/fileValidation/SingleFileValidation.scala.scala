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

  private val schemaZ = CsvSchema(pathOf("schema.yml"))

  def spec =
    suite("Single file validation")(nonAggSuite + aggSuite) @@ timeout(
      5.seconds
    )

  def nonAggSuite = suite("without aggregation")(
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
    test("empty file") {
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
    test("empty header line") {
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
    test("header mismatch") {
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
  )

  private val aggSink = ZSink
    .foldLeft[Byte, Foldable](Algo.md5.foldable)((acc, elem) =>
      acc(Array(elem))
    )
    .map(_.done)

  def aggSuite = suite("with aggregation")(
    test("successful validation") {
      for {
        schema <- schemaZ
        path = pathOf(
          Path("singleFileValidation") / "noAggregation" / "successful.csv"
        )
        validation <- CsvPathValidation.validateFileAndAggregate(
          schema,
          path
        )(aggSink)
      } yield assert(validation.hex)(equalTo("89d8ef80a9cf48f7ea38a82a8e1662c9"))
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
        schema <- schemaZ
        validation <- CsvPathValidation.validateFileAndAggregate(
          schema,
          path
        )(aggSink)
      } yield validation

      assertZIO(effect.flip)(equalTo(expectedError))
    }
  )
}
