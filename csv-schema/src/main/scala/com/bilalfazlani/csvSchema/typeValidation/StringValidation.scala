package com.bilalfazlani.csvSchema

import zio.prelude.Validation
import ColumnSchema.StringSchema

given Validator[StringSchema] = new Validator {
  extension (schema: StringSchema)
    def validate(value: String) = {
      def err(rule: ValidationRule) = FieldSchemaValidationError(
        schema.columnName,
        value,
        rule
      )

      val requiredError = err(ValidationRule.Required)
      val presenceValidationWhenRequired =
        Validation.fromPredicateWith(requiredError)(value)(v => !value.isBlank)

      val minLengthValidation =
        schema.minLength.fold(Validation.unit)(minLength =>
          Validation.fromPredicateWith(
            err(ValidationRule.MinLength(minLength))
          )(())(_ => value.length >= minLength)
        )

      val maxLengthValidation =
        schema.maxLength.fold(Validation.unit)(maxLength =>
          Validation.fromPredicateWith(
            err(ValidationRule.MaxLength(maxLength))
          )(())(_ => value.length <= maxLength)
        )

      val allowedValuesValidation =
        if schema.allowedValues.isEmpty
        then Validation.unit
        else
          Validation.fromPredicateWith(
            err(ValidationRule.AllowedValues(schema.allowedValues))
          )(())(_ => schema.allowedValues.contains(value.trim))

      val regexValdation =
        schema.regex.fold(Validation.unit)(regex =>
          Validation.fromPredicateWith(err(ValidationRule.Regex(regex)))(
            ()
          )(_ => regex matches value.trim)
        )

      val domainValidation = Validation.validate(
        minLengthValidation,
        maxLengthValidation,
        allowedValuesValidation,
        regexValdation
      )
      val flowWhenRequired = for {
        _ <- presenceValidationWhenRequired
        _ <- domainValidation
      } yield ()

      val flowWhenNotRequired =
        if value.isBlank then Validation.unit
        else domainValidation.map(_ => ())

      if schema.required then flowWhenRequired else flowWhenNotRequired
    }
}
