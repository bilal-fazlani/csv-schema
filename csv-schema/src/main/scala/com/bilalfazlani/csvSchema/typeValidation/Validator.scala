package com.bilalfazlani.csvSchema

import zio.prelude.Validation

trait Validator[S <: ColumnSchema[?]] {
  extension (s: S)
    def validate(value: String): Validation[FieldSchemaValidationError, Unit]
}

object Validator {
  def validate(schema: ColumnSchema[?], value: String) =
    schema match {
      case s: ColumnSchema.BooleanSchema => s.validate(value)
      case s: ColumnSchema.IntegerSchema => s.validate(value)
      case s: ColumnSchema.StringSchema  => s.validate(value)
    }
}

