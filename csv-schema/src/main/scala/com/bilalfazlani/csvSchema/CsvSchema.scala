package com.bilalfazlani.csvSchema

import zio.Console.*
import zio.*
import zio.config.ConfigDescriptor.*
import zio.config.*
import zio.config.magnolia.Descriptor
import zio.config.yaml.YamlConfig
import zio.config.yaml.YamlConfigSource
import zio.nio.file.Files
import zio.nio.file.Path
import scala.util.matching.Regex
import scala.util.Try
import zio.stream.ZStream
import zio.stream.ZPipeline

private given setDesc[A: Descriptor]: Descriptor[Set[A]] = Descriptor.from(
  Descriptor[List[A]].transform(_.toSet, _.toList)
)

private given regexDesc: Descriptor[Regex] = Descriptor.from(
  ConfigDescriptor.string.transformOrFailLeft[Regex](s =>
    Try(s.r).toEither.left.map(_.getMessage)
  )(r => r.regex)
)

// private given uniqueIndexDesc: Descriptor[UniqueIndex] =
//   summon[Descriptor[Set[String]]]

enum CsvDataType:
  case String, Integer, Boolean

sealed trait ColumnSchema derives Descriptor {
  val columnName: String
  val required: Boolean
  val dataType: CsvDataType
}

object ColumnSchema {
  case class StringSchema(
      columnName: String,
      maxLength: Option[Int] = None,
      minLength: Option[Int] = None,
      regex: Option[Regex] = None,
      required: Boolean = true,
      allowedValues: Set[String] = Set.empty
  ) extends ColumnSchema
      derives Descriptor {
    val dataType = CsvDataType.String

    override def equals(x: Any): Boolean = x match {
      case x: StringSchema =>
        this.columnName == x.columnName &&
        this.maxLength == x.maxLength &&
        this.minLength == x.minLength &&
        this.regex.map(_.toString) == x.regex.map(_.toString) &&
        this.required == x.required &&
        this.allowedValues == x.allowedValues
      case _ => false
    }
  }

  case class IntegerSchema(
      columnName: String,
      min: Option[Int] = None,
      max: Option[Int] = None,
      required: Boolean = true
  ) extends ColumnSchema
      derives Descriptor {
    val dataType = CsvDataType.Integer
  }

  case class BooleanSchema(
      columnName: String,
      required: Boolean = true
  ) extends ColumnSchema
      derives Descriptor {
    val dataType = CsvDataType.Boolean
  }
}

// opaque type UniqueIndex = Set[String]

// extension (index: UniqueIndex) def columns: Set[String] = index

// object UniqueIndex {
//   def apply(columnNames: Set[String]): UniqueIndex = columnNames
// }

sealed trait CsvSchema

object CsvSchema {
  case class Inline(columns: List[ColumnSchema]) extends CsvSchema
  case class File(path: Path) extends CsvSchema {
    private val configDescriptor: ConfigDescriptor[CsvSchema.Inline] =
      summon[Descriptor[CsvSchema.Inline]].desc.mapKey(
        toKebabCase
      )

    def load: IO[CsvFailure, CsvSchema.Inline] =
      ZIO.ifZIO(Files.exists(path))(
        ZStream
          .fromFile(path.toFile, 1024)
          .via(ZPipeline.utfDecode)
          .runCollect
          .map(_.mkString)
          .flatMap(str =>
            read(
              configDescriptor from YamlConfigSource
                .fromYamlString(
                  str,
                  "csv schema file"
                )
            )
          )
          .mapError(e => CsvFailure.SchemaParsingError(path, e)),
        ZIO.fail(CsvFailure.SchemaFileNotFound(path))
      )
  }
}
