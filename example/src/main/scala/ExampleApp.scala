import zio.*
import com.bilalfazlani.csvSchema.*
import zio.nio.file.Path
import zio.Console.*

object ExampleApp extends ZIOAppDefault {
  
  val program = for {
    schema <- CsvSchema.File(Path("./example/test.schema.yml")).load
    _ <- CsvValidation.validate(schema, Path("./example/data-invalid.csv"))
  } yield ()

  def run =
    program
      .provideSome[Scope](CsvValidation.live)
      .tapError(e => printLineError(e.toString))
      .zipRight(printLine("Valid data"))
      .exitCode
}
