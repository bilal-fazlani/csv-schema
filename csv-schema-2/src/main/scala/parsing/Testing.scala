package parsing

import Parser.given
import CsvDescriptor.*

opaque type Email = String

@main def testing = {
  // ---------- primitives ------------

  val ageDescriptor: IntegerDescriptor =
    int("age").requireMin(10).requireMax(50)

  val parsedAge = Parser.parse("", ageDescriptor)
  println(parsedAge)

  // ------------- options ------------

  val optionalAgeDescriptor: CsvDescriptor[Option[Int]] = ageDescriptor.optional

  val parsedAge2 = Parser.parse("20", optionalAgeDescriptor)
  println(parsedAge2)

  // --------- custom data types ------

  // --------- case classes -----------

  case class Person(name: String, age: Int, email: Email)
}
