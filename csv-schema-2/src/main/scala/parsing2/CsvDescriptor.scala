package parsing2

import zio.prelude.Validation

trait Error
object Error extends Error

class CsvDescriptor[A](val name: String, val validation: Validation[Error, A]) {
  def map[B](f: A => B): CsvDescriptor[B] =
    CsvDescriptor(name, validation.map(f))

  def flatMap[B](f: A => CsvDescriptor[B]): CsvDescriptor[B] =
    // CsvDescriptor(name, )
    ???
    //validation.flatMap(f))

  def optional: CsvDescriptor[Option[A]] = ???

  def withValidation(newValidation: Validation[Error, Unit]): CsvDescriptor[A] =
    CsvDescriptor(
      name,
      Validation.validate(validation, newValidation).map(_._1)
    )

  def from(source: String): CsvDescriptorWithSource[A] =
    CsvDescriptorWithSource(source, this)
}

class CsvDescriptorWithSource[A](source: String, desc: CsvDescriptor[A])
    extends CsvDescriptor[A](desc.name, desc.validation)

object CsvDescriptor {
  def pure[A](name: String, value: A) =
    CsvDescriptor(name, Validation.succeed(value))

  def string(name: String) =
    (str: String) =>
      CsvDescriptor(
        name,
        Validation.fromPredicate(str)(!_.isBlank).mapError(_ => Error)
      )

  // def integer(name: String): CsvDescriptor[String] =
  //   (int: Int) =>
  //     CsvDescriptor(
  //       name,
  //       Validation.fromPredicate(value)(!_.isBlank).mapError(_ => Error)
  //     )
}
