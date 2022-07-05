package com.bilalfazlani.csvSchema

import zio.prelude.Validation
import ColumnSchema.DoubleSchema

given Validator[DoubleSchema] = new Validator {
  extension (schema: DoubleSchema)
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

      def minValidation(number: Double) =
        schema.min.fold(Validation.unit)(min =>
          Validation.fromPredicateWith(err(ValidationRule.Min(min)))(())(_ =>
            number >= min
          )
        )

      def maxValidation(number: Double) =
        schema.max.fold(Validation.unit)(max =>
          Validation.fromPredicateWith(err(ValidationRule.Max(max)))(())(_ =>
            number <= max
          )
        )

      def domainValidation(number: Double) =
        Validation.validate(minValidation(number), maxValidation(number))

      val doubleError = err(ValidationRule.Parse(CsvDataType.Double))

      val requiredFlow = for {
        str <- presenceValidationWhenRequired
        double <- Validation.fromOptionWith(doubleError)(str.trim.toDoubleOption)
        domain <- domainValidation(double)
      } yield ()

      val nonRequiredFlow =
        if (value.isBlank)
        then Validation.unit
        else
          for {
            double <- Validation.fromOptionWith(doubleError)(value.trim.toDoubleOption)
            domain <- domainValidation(double)
          } yield ()

      if (schema.required) requiredFlow else nonRequiredFlow
}
