package com.bilalfazlani.csvSchema
package typeValidation

import zio.*
import zio.test.*
import zio.test.Assertion.*
import ColumnSchema.StringSchema
import zio.prelude.Validation

object StringSchemaValidation extends ZIOSpecDefault {
  def spec = suite("String schema validation tests")(
    test(
      "check should pass for a valid value when no validations are specified"
    ) {
      val schema =
        ColumnSchema.StringSchema("name", None, None, None, false, Set.empty)
      val result = schema.validate("john").toEither
      assertTrue(result == Right(()))
    },
    test(
      "check should pass for a valid value when value is not required, but domain validations are present"
    ) {
      val schema = ColumnSchema
        .StringSchema(
          "name",
          Some(100),
          Some(2),
          Some("[a-z]*".r),
          false,
          Set("john", "jane")
        )
      val result = schema.validate("john").toEither
      assertTrue(result == Right(()))
    },
    test(
      "check should pass for a valid value when value is required, and domain validations are present"
    ) {
      val schema = ColumnSchema
        .StringSchema(
          "name",
          Some(100),
          Some(2),
          Some("[a-z]*".r),
          true,
          Set("john", "jane")
        )
      val result = schema.validate("john").toEither
      assertTrue(result == Right(()))
    },
    test(
      "check should fail with domain errors for an invalid (and required) domain value"
    ) {
      val column = "name"
      val min = 4
      val max = 100
      val regex = "[a-z]*".r
      val allowed = Set("john", "jane")
      val value = "j2"

      val schema = ColumnSchema
        .StringSchema(
          column,
          Some(max),
          Some(min),
          Some(regex),
          true,
          allowed
        )

      val result: Either[List[FieldSchemaValidationError], Unit] =
        schema.validate(value).toEither.left.map(_.toList)

      def rule(rule: ValidationRule) =
        FieldSchemaValidationError(column, value, rule)

      val expected: List[FieldSchemaValidationError] = List(
        rule(ValidationRule.MinLength(min)),
        rule(ValidationRule.AllowedValues(allowed)),
        rule(ValidationRule.Regex(regex))
      )
      assert(result)(
        isLeft(
          Assertion.hasSameElements(expected)
        )
      )
    },
    test(
      "check should fail with domain errors for an invalid (and optional) domain value"
    ) {
      val column = "name"
      val min = 3
      val max = 4
      val regex = "[a-z]*".r
      val allowed = Set("john", "jane")
      val value = "j23ff"

      val schema = ColumnSchema
        .StringSchema(
          column,
          Some(max),
          Some(min),
          Some(regex),
          false,
          allowed
        )

      val result: Either[List[FieldSchemaValidationError], Unit] =
        schema.validate(value).toEither.left.map(_.toList)

      def rule(rule: ValidationRule) =
        FieldSchemaValidationError(column, value, rule)

      val expected: List[FieldSchemaValidationError] = List(
        rule(ValidationRule.MaxLength(max)),
        rule(ValidationRule.AllowedValues(allowed)),
        rule(ValidationRule.Regex(regex))
      )
      assert(result)(
        isLeft(
          Assertion.hasSameElements(expected)
        )
      )
    },
    test(
      "check should fail with required error when value is required but not provided"
    ) {
      val column = "name"
      val min = 3
      val max = 4
      val regex = "[a-z]*".r
      val allowed = Set("john", "jane")
      val value = ""

      val schema = ColumnSchema
        .StringSchema(
          column,
          Some(max),
          Some(min),
          Some(regex),
          true,
          allowed
        )

      val result: Either[List[FieldSchemaValidationError], Unit] =
        schema.validate(value).toEither.left.map(_.toList)

      def rule(rule: ValidationRule) =
        FieldSchemaValidationError(column, value, rule)

      val expected: List[FieldSchemaValidationError] = List(
        rule(ValidationRule.Required)
      )
      assert(result)(
        isLeft(
          Assertion.hasSameElements(expected)
        )
      )
    },
    test(
      "check should pass when no value is required and no value is provided, but domain validations are specified"
    ) {
      val column = "name"
      val min = 3
      val max = 4
      val regex = "[a-z]*".r
      val allowed = Set("john", "jane")
      val value = ""

      val schema = ColumnSchema
        .StringSchema(
          column,
          Some(max),
          Some(min),
          Some(regex),
          false,
          allowed
        )

      val result: Either[List[FieldSchemaValidationError], Unit] =
        schema.validate(value).toEither.left.map(_.toList)

      def rule(rule: ValidationRule) =
        FieldSchemaValidationError(column, value, rule)

      assert(result)(
        isRight(
          Assertion.isUnit
        )
      )
    },
  )
}
