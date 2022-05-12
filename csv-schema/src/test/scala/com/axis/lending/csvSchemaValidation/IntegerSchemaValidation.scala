package com.bilalfazlani.csvSchema

import zio.*
import zio.test.*
import zio.test.Assertion.*
import ColumnSchema.IntegerSchema
import zio.prelude.Validation

object IntegerSchemaValidation extends ZIOSpecDefault {
  def spec = suite("Integer schema validation tests")(
    test(
      "check should pass for a valid value when no validations are specified"
    ) {
      val schema = ColumnSchema.IntegerSchema("age", None, None, false)
      val result = schema.validate("2").toEither
      assertTrue(result == Right(()))
    },
    test(
      "check should pass for a valid value when value is not required"
    ) {
      val schema = ColumnSchema.IntegerSchema("age", Some(1), Some(10), true)
      val result = schema.validate("2").toEither
      assertTrue(result == Right(()))
    },
    test(
      "check should fail with domain errors for an invalid (and required) domain value"
    ) {
      val schema = ColumnSchema.IntegerSchema("age", Some(100), Some(10), true)
      val result = schema.validate("50").toEither
      assertTrue(
        result == Left(
          NonEmptyChunk(
            FieldSchemaValidationError("age", "50", ValidationRule.Min(100)),
            FieldSchemaValidationError("age", "50", ValidationRule.Max(10))
          )
        )
      )
    },
    test(
      "check should fail with domain errors for an invalid (and non required) domain value"
    ) {
      val schema = ColumnSchema.IntegerSchema("age", Some(100), Some(10), false)
      val result = schema.validate("50").toEither
      assertTrue(
        result == Left(
          NonEmptyChunk(
            FieldSchemaValidationError("age", "50", ValidationRule.Min(100)),
            FieldSchemaValidationError("age", "50", ValidationRule.Max(10))
          )
        )
      )
    },
    test(
      "check should fail with required error when value is required but not provided"
    ) {
      val schema = ColumnSchema.IntegerSchema("age", None, None, true)
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
        ColumnSchema.IntegerSchema("age", Some(100), Some(10), false)
      val result = schema.validate("").toEither
      assertTrue(
        result == Right(())
      )
    },
    test(
      "check should fail with parsing error when value is not required but an invalid value is passed"
    ) {
      val schema =
        ColumnSchema.IntegerSchema("age", Some(100), Some(10), false)
      val result = schema.validate("a").toEither
      assertTrue(
        result == Left(
          NonEmptyChunk(
            FieldSchemaValidationError(
              "age",
              "a",
              ValidationRule.Parse(CsvDataType.Integer)
            )
          )
        )
      )
    },
    test(
      "check should fail with parsing error when value is required and an invalid value is passed"
    ) {
      val schema =
        ColumnSchema.IntegerSchema("age", Some(100), Some(10), true)
      val result = schema.validate("a").toEither
      assertTrue(
        result == Left(
          NonEmptyChunk(
            FieldSchemaValidationError(
              "age",
              "a",
              ValidationRule.Parse(CsvDataType.Integer)
            )
          )
        )
      )
    }
  )
}
