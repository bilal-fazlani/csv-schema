package parsing

import zio.prelude.Validation
import CsvDescriptor.*

enum CsvError:
  case ValueNotFound
  case ReaderError
  case Validation(str: String)

type Reader[A] = String => Option[A]

object Reader {
  def apply[A](using reader: Reader[A]) = reader
}

trait Parser[A: Reader] {
  def parse(
      descriptor: CsvDescriptor[A],
      value: String
  ): Validation[CsvError, A]
}

object Parser {
  def parse[A: Parser](
      value: String,
      descriptor: CsvDescriptor[A]
  ): Validation[CsvError, A] =
    val parser = summon[Parser[A]]
    parser.parse(descriptor, value)

  given stringParser: Parser[String] = new Parser[String] {
    override def parse(
        descriptor: CsvDescriptor[String],
        value: String
    ): Validation[CsvError, String] =
      val strSchema = descriptor.asInstanceOf[StringDescriptor]

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
        descriptor: CsvDescriptor[Int],
        value: String
    ): Validation[CsvError, Int] =
      val intSchema = descriptor.asInstanceOf[IntegerDescriptor]

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
        descriptor: CsvDescriptor[Boolean],
        value: String
    ): Validation[CsvError, Boolean] =
      val intSchema = descriptor.asInstanceOf[BooleanDescriptor]

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

  given Reader[String] = (s) => if s.isBlank then None else Some(s.trim)
  given Reader[Int] = (s) => s.trim.toIntOption
  given Reader[Boolean] = (s) => s.trim.toBooleanOption

  given optionReader[A: Reader]: Reader[Option[A]] = (s: String) => {
    val reader = summon[Reader[A]]
    val mayBeA: Option[A] = reader(s)
    Some(mayBeA)
  }

  given optionParser[A: Parser: Reader]: Parser[Option[A]] =
    new Parser[Option[A]] {
      def parse(
          descriptor: CsvDescriptor[Option[A]],
          value: String
      ): Validation[CsvError, Option[A]] = {
        val parser = summon[Parser[A]]

        val optionalDescriptor =
          descriptor.asInstanceOf[CsvDescriptor.OptionalDescriptor[A]]

        if (!value.isBlank) then
          parser
            .parse(optionalDescriptor.descriptor, value)
            .map(a => Some(a))
        else Validation.succeed(None)
      }
    }
}
