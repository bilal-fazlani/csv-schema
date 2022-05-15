package com.bilalfazlani.csvSchema

import zio.prelude.Validation
import ColumnSchema.IntegerSchema

given Validator[IntegerSchema] = new Validator {
  extension (schema: IntegerSchema)
    def validate(value: String) =
      def err(rule: ValidationRule) = FieldSchemaValidationError(
        schema.columnName,
        value,
        rule
      )

      val requiredError = err(ValidationRule.Required)
      val presenceValidationWhenRequired =
        Validation.fromPredicateWith(requiredError)(value)(v => !value.isBlank)

      val requiredValidationWhenNotRequired = Validation.unit

      def minValidation(number: Int) =
        schema.min.fold(Validation.unit)(min =>
          Validation.fromPredicateWith(err(ValidationRule.Min(min)))(())(_ =>
            number >= min
          )
        )

      def maxValidation(number: Int) =
        schema.max.fold(Validation.unit)(max =>
          Validation.fromPredicateWith(err(ValidationRule.Max(max)))(())(_ =>
            number <= max
          )
        )

      def domainValidation(number: Int) =
        Validation.validate(minValidation(number), maxValidation(number))

      val intError = err(ValidationRule.Parse(CsvDataType.Integer))

      val requiredFlow = for {
        str <- presenceValidationWhenRequired
        int <- Validation.fromOptionWith(intError)(str.trim.toIntOption)
        domain <- domainValidation(int)
      } yield ()

      val nonRequiredFlow =
        if (value.isBlank)
        then Validation.unit
        else
          for {
            int <- Validation.fromOptionWith(intError)(value.trim.toIntOption)
            domain <- domainValidation(int)
          } yield ()

      if (schema.required) requiredFlow else nonRequiredFlow
}
