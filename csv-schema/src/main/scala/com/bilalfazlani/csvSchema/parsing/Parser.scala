package com.bilalfazlani.csvSchema

import zio.prelude.Validation
import ColumnSchema.*

enum CsvError:
  case ValueNotFound
  case ReaderError
  case Validation(str: String)

type Reader[A] = String => Option[A]

object Reader {
  def apply[A](using reader: Reader[A]) = reader
}

trait Parser[A: Reader] {
  def parse(schema: ColumnSchema[A], value: String): Validation[CsvError, A]
}

object Parser {

  def parse[A: Parser](
      schema: ColumnSchema[A],
      value: String
  ): Validation[CsvError, A] =
    summon[Parser[A]].parse(schema, value)

  given Reader[String] = (s) => if s.isBlank then None else Some(s.trim)
  given Reader[Int] = (s) => s.trim.toIntOption
  given Reader[Boolean] = (s) => s.trim.toBooleanOption

  given stringParser: Parser[String] = new Parser[String] {
    override def parse(
        schema: ColumnSchema[String],
        value: String
    ): Validation[CsvError, String] =
      val strSchema = schema.asInstanceOf[StringSchema]

      val emptyValidation =
        Validation.fromPredicateWith(CsvError.ValueNotFound)(value)(!_.isBlank)

      val minLengthValidation =
        strSchema.minLength.fold(Validation.succeed(value))(minLength =>
          Validation.fromPredicateWith(
            CsvError.Validation("min value")
          )(value)(v => v.trim.length >= minLength)
        )

      val maxLengthValidation =
        strSchema.maxLength.fold(Validation.succeed(value))(maxLength =>
          Validation.fromPredicateWith(
            CsvError.Validation("max value")
          )(value)(_ => value.trim.length <= maxLength)
        )

      val regexValdation =
        strSchema.regex.fold(Validation.succeed(value))(regex =>
          Validation.fromPredicateWith(CsvError.Validation("regex"))(
            value
          )(v => regex matches v.trim)
        )

      emptyValidation
        .flatMap(_ =>
          Validation
            .validate(
              minLengthValidation,
              maxLengthValidation,
              regexValdation
            )
        )
        .map(_ => value)
  }

  given intParser: Parser[Int] = new Parser[Int] {
    override def parse(
        schema: ColumnSchema[Int],
        value: String
    ): Validation[CsvError, Int] =
      val intSchema = schema.asInstanceOf[IntegerSchema]

      val emptyValidation =
        Validation.fromPredicateWith(CsvError.ValueNotFound)(value)(!_.isBlank)

      val parseValidation =
        Validation.fromOptionWith(CsvError.ReaderError)(Reader[Int](value.trim))

      def minValidation(number: Int) =
        intSchema.min.fold(Validation.succeed(number))(min =>
          Validation.fromPredicateWith(CsvError.Validation("min"))(number)(n =>
            n >= min
          )
        )

      def maxValidation(number: Int) =
        intSchema.max.fold(Validation.succeed(number))(max =>
          Validation.fromPredicateWith(CsvError.Validation("max"))(number)(n =>
            n <= max
          )
        )

      for {
        str <- emptyValidation
        number <- parseValidation
        _ <- Validation.validate(minValidation(number), maxValidation(number))
      } yield number
  }

  given boolParser: Parser[Boolean] = new Parser[Boolean] {
    override def parse(
        schema: ColumnSchema[Boolean],
        value: String
    ): Validation[CsvError, Boolean] =
      val intSchema = schema.asInstanceOf[BooleanSchema]

      val emptyValidation =
        Validation.fromPredicateWith(CsvError.ValueNotFound)(value)(!_.isBlank)

      val parseValidation =
        Validation.fromOptionWith(CsvError.ReaderError)(
          Reader[Boolean](value.trim)
        )

      for {
        str <- emptyValidation
        boolean <- parseValidation
      } yield boolean
  }
}

@main def letsgo = {
  val parsed = Parser.parse(BooleanSchema("abcd", false), "false")
  println(parsed)
}
