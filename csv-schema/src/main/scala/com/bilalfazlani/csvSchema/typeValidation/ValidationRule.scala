package com.bilalfazlani.csvSchema

import scala.util.matching.{Regex => SRegex}

enum ValidationRule:
  case Parse(dataType: CsvDataType)
  case MaxLength(value: Int)
  case MinLength(value: Int)
  case Regex(value: SRegex)
  case AllowedValues(values: Set[String])
  case Min(value: Long)
  case Max(value: Long)
  case Required

case class FieldSchemaValidationError(
    field: String,
    value: String,
    rule: ValidationRule
)
