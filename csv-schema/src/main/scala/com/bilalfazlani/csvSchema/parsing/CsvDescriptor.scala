package com.bilalfazlani.csvSchema.parsing

import scala.util.matching.Regex

sealed trait CsvDescriptor[A] {
  def optional: CsvDescriptor[Option[A]] = CsvDescriptor.OptionalDescriptor(
    this
  )
}

object CsvDescriptor {
  case class OptionalDescriptor[A](
      descriptor: CsvDescriptor[A]
  ) extends CsvDescriptor[Option[A]]

  case class StringDescriptor(
      maxLength: Option[Int],
      minLength: Option[Int],
      regex: Option[Regex],
      allowedValues: Set[String] = Set.empty
  ) extends CsvDescriptor[String]

  case class IntegerDescriptor(
      min: Option[Int],
      max: Option[Int]
  ) extends CsvDescriptor[Int]

  case class BooleanDescriptor(
  ) extends CsvDescriptor[Boolean]

}
