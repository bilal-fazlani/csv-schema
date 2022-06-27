import zio.*
import com.bilalfazlani.csvSchema.*
import zio.nio.file.Path
import zio.Console.*

object ExampleApp extends ZIOAppDefault {
  def run =
    val result = for {
      schema <- CsvSchema(Path("./example/test.schema.yml"))
      result <- CsvValidationLive
        .validate(schema, Path("./example/data2.csv"))
    } yield result

    result.foldZIO(printLineError(_), _ => printLine("Success"))
}
