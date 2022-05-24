package parsing

import scala.util.matching.Regex
import zio.prelude.Validation

trait CsvDescriptor[A] {
  val name: String
  def optional: CsvDescriptor[Option[A]] =
    CsvDescriptor.OptionalDescriptor(name, this)
}

object CsvDescriptor {

  def fromString[A](f: String => Validation[CsvError, A]): CsvDescriptor[A] = ???

  def int(name: String): IntegerDescriptor = IntegerDescriptor(
    name,
    None,
    None
  )

  case class OptionalDescriptor[A](
      name: String,
      descriptor: CsvDescriptor[A]
  ) extends CsvDescriptor[Option[A]]

  case class StringDescriptor(
      name: String,
      maxLength: Option[Int],
      minLength: Option[Int],
      regex: Option[Regex],
      allowedValues: Set[String] = Set.empty
  ) extends CsvDescriptor[String]

  case class IntegerDescriptor(
      name: String,
      min: Option[Int],
      max: Option[Int]
  ) extends CsvDescriptor[Int] {
    def requireMin(min: Int) = this.copy(min = Some(min))
    def requireMax(max: Int) = this.copy(max = Some(max))
  }

  case class BooleanDescriptor(
      name: String
  ) extends CsvDescriptor[Boolean]

}
