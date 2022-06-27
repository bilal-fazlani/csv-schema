import zio.*
import com.bilalfazlani.csvSchema.*
import zio.nio.file.Path
import zio.Console.*
import ColumnSchema.*
import com.bilalfazlani.csvSchema.CsvSchema.Inline

object ExampleWithInlineSchema extends ZIOAppDefault {

  val schema: CsvSchema = StringSchema(
    columnName = "name",
    maxLength = Some(100),
    minLength = Some(3),
    regex = Some("[a-zA-Z]*".r)
  ) &
    StringSchema(
      columnName = "city",
      allowedValues = Set("Mumbai", "Pune", "Delhi")
    ) &
    BooleanSchema(columnName = "selfEmployed") &
    IntegerSchema(
      columnName = "age",
      min = Some(10),
      max = Some(100),
      required = false
    )

  def run =
    CsvValidation
      .validate(schema, Path("./example/data-invalid.csv"))
      .provideSome[Scope](CsvValidation.live)
      .tapError(e => printLineError(e.toString))
      .zipRight(printLine("Valid data"))
      .exitCode
}
