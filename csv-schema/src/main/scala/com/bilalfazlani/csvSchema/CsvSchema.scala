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
      maxLength: Option[Int],
      minLength: Option[Int],
      regex: Option[Regex],
      required: Boolean = true,
      allowedValues: Set[String] = Set.empty
  ) extends ColumnSchema
      derives Descriptor {
    val dataType = CsvDataType.String
  }

  case class IntegerSchema(
      columnName: String,
      min: Option[Int],
      max: Option[Int],
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

case class CsvSchema(
    columns: List[ColumnSchema]
    // uniqueCombinations: Set[UniqueIndex] = Set.empty
)

object CsvSchema {
  private val configDescriptor: ConfigDescriptor[CsvSchema] =
    summon[Descriptor[CsvSchema]].desc.mapKey(
      toKebabCase
    )

  def apply(path: Path): IO[CsvFailure, CsvSchema] =
    ZIO.ifZIO(Files.exists(path))(
      read(
        CsvSchema.configDescriptor from YamlConfigSource
          .fromYamlPath(
            path.toFile.toPath
          )
      ).mapError(e => CsvFailure.SchemaParsingError(path, e)),
      ZIO.fail(CsvFailure.SchemaFileNotFound(path))
    )
}
