import zio.*
import com.bilalfazlani.csvSchema.*
import zio.nio.file.Path
import zio.Console.*

object ExampleApp extends ZIOAppDefault {
  def run =
    val result = for {
      schema <- CsvSchema(Path("./example/test.schema.yml"))
      validationResult <- CsvPathValidation
        .validateFile(Path("./example/data.csv"), schema)
    } yield validationResult

    result.foldZIO(printLineError(_), _ => printLine("Success"))
}
