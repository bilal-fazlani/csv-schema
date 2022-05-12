package com.bilalfazlani.csvSchema

import zio.prelude.Validation
import ColumnSchema.BooleanSchema

given Validator[BooleanSchema] = new Validator {
  extension (schema: BooleanSchema)
    def validate(value: String) =
      def err(rule: ValidationRule) = FieldSchemaValidationError(
        schema.columnName,
        value,
        rule
      )
      val boolError = err(ValidationRule.Parse(CsvDataType.Boolean))
      val requiredError = err(ValidationRule.Required)

      val presenceValidationWhenRequired =
        Validation.fromPredicateWith(requiredError)(value)(v => !value.isBlank)

      val flowWhenRequired = for {
        str <- presenceValidationWhenRequired
        bool <- Validation.fromOptionWith(boolError)(str.trim.toBooleanOption)
      } yield ()

      val flowWhenNotRequired =
        if value.isBlank then Validation.unit
        else
          Validation
            .fromOptionWith(boolError)(value.trim.toBooleanOption)
            .map(_ => ())

      if schema.required
      then flowWhenRequired
      else flowWhenNotRequired
}
