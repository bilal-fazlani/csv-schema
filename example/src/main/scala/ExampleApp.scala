import zio.*
import com.bilalfazlani.csvSchema.*
import zio.nio.file.Path
import zio.Console.*

object ExampleApp extends ZIOAppDefault {
  def run =
    CsvSchema(Path("./example/test.schema.yml"))
      .flatMap(schema =>
        CsvValidation.validate(schema, Path("./example/data-invalid.csv"))
      )
      .provideSome[Scope](CsvValidation.live)
      .tapError(e => printLineError(e.toString))
      .zipRight(printLine("Valid data"))
      .exitCode
}
