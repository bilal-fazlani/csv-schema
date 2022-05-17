package com.bilalfazlani.csvSchema
package typeValidation

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.prelude.Validation

object BooleanSchemaValidation extends ZIOSpecDefault {
  def spec = suite("Boolean schema validation tests")(
    test(
      "check should pass for a valid value when no validations are specified"
    ) {
      val schema = ColumnSchema.BooleanSchema("isOnSale", false)
      val result = schema.validate("true").toEither
      assertTrue(result == Right(()))
    },
    test(
      "check should pass for a valid value when value is not required"
    ) {
      val schema = ColumnSchema.BooleanSchema("isOnSale", true)
      val result = schema.validate("false").toEither
      assertTrue(result == Right(()))
    },
    test(
      "check should fail with required error when value is required but not provided"
    ) {
      val schema = ColumnSchema.BooleanSchema("isOnSale", true)
      val result = schema.validate("").toEither
      assertTrue(
        result == Left(
          NonEmptyChunk(
            FieldSchemaValidationError("isOnSale", "", ValidationRule.Required)
          )
        )
      )
    },
    test(
      "check should pass when no value is required and no value is provided"
    ) {
      val schema =
        ColumnSchema.BooleanSchema("isOnSale", false)
      val result = schema.validate("").toEither
      assertTrue(
        result == Right(())
      )
    },
    test(
      "check should fail with parsing error when value is not required but an invalid value is passed"
    ) {
      val schema =
        ColumnSchema.BooleanSchema("isOnSale", false)
      val result = schema.validate("a").toEither
      assertTrue(
        result == Left(
          NonEmptyChunk(
            FieldSchemaValidationError(
              "isOnSale",
              "a",
              ValidationRule.Parse(CsvDataType.Boolean)
            )
          )
        )
      )
    },
    test(
      "check should fail with parsing error when value is required and an invalid value is passed"
    ) {
      val schema =
        ColumnSchema.BooleanSchema("isOnSale", true)
      val result = schema.validate("a").toEither
      assertTrue(
        result == Left(
          NonEmptyChunk(
            FieldSchemaValidationError(
              "isOnSale",
              "a",
              ValidationRule.Parse(CsvDataType.Boolean)
            )
          )
        )
      )
    }
  ) @@ TestAspect.timeout(5.seconds)
}
