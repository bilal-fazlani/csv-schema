package com.bilalfazlani.csvSchema

import zio.nio.file.Path

import scala.util.Properties

enum CsvFailure:
  case SchemaValidationError(
      file: Path,
      lineNumber: Long,
      fieldErrors: Seq[FieldSchemaValidationError]
  )
  case SyntaxValidationError(
      file: Path,
      lineNumber: Long,
      errorMessage: String
  )
  case SchemaFileNotFound(file: Path)
  case CsvReadingError(file: Path, cause: Throwable)
  case SchemaParsingError(file: Path, cause: Throwable)
  case ProcessingError(file: Path, cause: Throwable)
  case Multiple(errors: Seq[CsvFailure])

  infix def +(e: CsvFailure): CsvFailure.Multiple = (this, e) match {
    case (CsvFailure.Multiple(e1), CsvFailure.Multiple(e2)) =>
      CsvFailure.Multiple(e1 ++ e2)
    case (CsvFailure.Multiple(e1), e2) => CsvFailure.Multiple(e1.appended(e2))
    case (e1, CsvFailure.Multiple(e2)) =>
      CsvFailure.Multiple(e2.prepended(e1))
    case (e1, e2) => CsvFailure.Multiple(Seq(e1, e2))
  }

  private val newLine = Properties.lineSeparator
  override def toString: String = this match {
    case CsvFailure.Multiple(seq) =>
      seq.foldRight[String]("")((e, str) =>
        str + Properties.lineSeparator + e.toString
      )

    case CsvFailure.SchemaParsingError(path, e) =>
      s"could not parse schema at $path"

    case CsvFailure.CsvReadingError(path, cause) =>
      s"could not read file $path"

    case CsvFailure.SchemaFileNotFound(path) =>
      s"schema file not found at ${path}"

    case CsvFailure.SyntaxValidationError(path, lineNumber, message) =>
      val top = s"syntax error at ${path}:$lineNumber"
      val bottom = pad(message, 1)
      top + newLine + bottom

    case CsvFailure.SchemaValidationError(path, lineNumber, errors) =>
      val top = s"validation failed at ${path}:$lineNumber"
      val children = errors.map(e => showFieldValidationError(e))
      children.prepended(top).mkString(newLine)

    case CsvFailure.ProcessingError(path, cause) =>
      val top = s"error occured while executing aggregate sink for file - $path"
      val children = cause.getStackTrace
        .map(st => "  " + st.toString)
      children.prepended(top).mkString(newLine)
  }

  private def pad(v: String, level: Int): String = ("  - " * level) + v

  private def showFieldValidationError(
      error: FieldSchemaValidationError
  ): String = error match {
    case FieldSchemaValidationError(
          field: String,
          value: String,
          rule: ValidationRule
        ) =>
      pad(
        rule match {
          // ALL
          case ValidationRule.Parse(dtype) =>
            s"$field '${value.trim}' is not a valid $dtype"
          case ValidationRule.Required =>
            s"missing value for `$field`"

          // STRING
          case ValidationRule.MaxLength(max) =>
            s"$field '${value.trim}' is of length ${value.length}. expected length to be <= $max"
          case ValidationRule.MinLength(min) =>
            s"$field '${value.trim}' is of length ${value.length}. expected length to be >= $min"
          case ValidationRule.Regex(reg) =>
            s"$field '${value.trim}' did not match regex: $reg"
          case ValidationRule.AllowedValues(values) =>
            s"$field '${value.trim}' is invalid. valid values are: ${values.mkString(", ")}"

          // NUMBER
          case ValidationRule.Min(min) =>
            s"$field '${value.trim}' should be >= $min"
          case ValidationRule.Max(max) =>
            s"$field '${value.trim}' should be <= $max"
        },
        1
      )
  }
