package com.bilalfazlani.csvSchema
package typeValidation

import zio.*
import zio.test.*
import zio.test.Assertion.*
import ColumnSchema.DoubleSchema
import zio.prelude.Validation

object DoubleSchemaValidation extends ZIOSpecDefault {
  def spec = suite("Double schema validation tests")(
    test(
      "check should pass for a valid value when no validations are specified"
    ) {
      val schema = ColumnSchema.DoubleSchema("age", None, None, false)
      val result = schema.validate("2").toEither
      assertTrue(result == Right(()))
    },
    test(
      "check should pass for a valid value when value is not required"
    ) {
      val schema = ColumnSchema.DoubleSchema("age", Some(1.2), Some(10.55), true)
      val result = schema.validate("2.4").toEither
      assertTrue(result == Right(()))
    },
    test(
      "check should fail with domain errors for an invalid (and required) domain value"
    ) {
      val schema = ColumnSchema.DoubleSchema("age", Some(100.33), Some(10.555), true)
      val result = schema.validate("50.1112").toEither
      assertTrue(
        result == Left(
          NonEmptyChunk(
            FieldSchemaValidationError("age", "50.1112", ValidationRule.Min(100.33)),
            FieldSchemaValidationError("age", "50.1112", ValidationRule.Max(10.555))
          )
        )
      )
    },
    test(
      "check should fail with domain errors for an invalid (and non required) domain value"
    ) {
      val schema = ColumnSchema.DoubleSchema("age", Some(100.33), Some(10.663), false)
      val result = schema.validate("50.01").toEither
      assertTrue(
        result == Left(
          NonEmptyChunk(
            FieldSchemaValidationError("age", "50.01", ValidationRule.Min(100.33)),
            FieldSchemaValidationError("age", "50.01", ValidationRule.Max(10.663))
          )
        )
      )
    },
    test(
      "check should fail with required error when value is required but not provided"
    ) {
      val schema = ColumnSchema.DoubleSchema("age", None, None, true)
      val result = schema.validate("").toEither
      assertTrue(
        result == Left(
          NonEmptyChunk(
            FieldSchemaValidationError("age", "", ValidationRule.Required)
          )
        )
      )
    },
    test(
      "check should pass when no value is required and no value is provided, but domain validations are specified"
    ) {
      val schema =
        ColumnSchema.DoubleSchema("age", Some(100.008), Some(10), false)
      val result = schema.validate("").toEither
      assertTrue(
        result == Right(())
      )
    },
    test(
      "check should fail with parsing error when value is not required but an invalid value is passed"
    ) {
      val schema =
        ColumnSchema.DoubleSchema("age", Some(100.009), Some(10), false)
      val result = schema.validate("a").toEither
      assertTrue(
        result == Left(
          NonEmptyChunk(
            FieldSchemaValidationError(
              "age",
              "a",
              ValidationRule.Parse(CsvDataType.Double)
            )
          )
        )
      )
    },
    test(
      "check should fail with parsing error when value is required and an invalid value is passed"
    ) {
      val schema =
        ColumnSchema.DoubleSchema("age", Some(100), Some(10.1), true)
      val result = schema.validate("a").toEither
      assertTrue(
        result == Left(
          NonEmptyChunk(
            FieldSchemaValidationError(
              "age",
              "a",
              ValidationRule.Parse(CsvDataType.Double)
            )
          )
        )
      )
    }
  ) @@ TestAspect.timeout(5.seconds)
}
