# CSV Schema

![Maven Central](https://img.shields.io/maven-central/v/com.bilal-fazlani/csv-schema_3?color=blue&label=Latest%20Version&style=for-the-badge)

Validate csv files agaists a schema

### Dependencies

Supports Scala 3 and ZIO 2

```scala
libraryDependencies += "com.bilal-fazlani" %% "csv-schema" % "<VERSION>"
```

### Example

Schema file

```yml
columns:
  - string-schema:
      column-name: name
      max-length: 100
      min-length: 3
      regex: "[a-ZA-Z]*"
  - string-schema:
      column-name: city
      allowed-values: 
        - Mumbai
        - Pune
        - Delhi
  - boolean-schema:
      column-name: selfEmployed
  - integer-schema:
      column-name: age
      min: 10
      max: 100
      required: false
  - double-schema:
      column-name: salary
      min: 999.00534
      max: 80000
      required: false      
```

CSV file

```csv
name, city, selfEmployed, age
,,,

as, Tokyo, dddd, 150
john,
ab cd, Delhi, false, 2
asasd, Mumbai, true, 20
```

```scala
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
```

You can also create schema programatically inline

```scala
  val schema: CsvSchema = ColumnSchema.StringSchema(
    columnName = "name",
    maxLength = Some(100),
    minLength = Some(3),
    regex = Some("[a-zA-Z]*".r)
  ) &
    ColumnSchema.StringSchema(
      columnName = "city",
      allowedValues = Set(
        "Mumbai",
        "Pune",
        "Delhi"
      )
    ) &
    ColumnSchema.BooleanSchema(
      columnName = "selfEmployed"
    ) &
    ColumnSchema.IntegerSchema(
      columnName = "age",
      min = Some(10),
      max = Some(100),
      required = false
    ) &
    ColumnSchema.DoubleSchema(
      columnName = "salary",
      min = Some(1000.0002),
      max = Some(8000.999),
      required = false
    )
```

Error reporting

```
validation failed at ./test.csv:2
  - missing value for `name`
  - missing value for `city`
  - missing value for `selfEmployed`
syntax error at ./test.csv:3
  - blank line
validation failed at ./test.csv:4
  - name 'as' is of length 2. expected length to be >= 3
  - city 'Tokyo' is invalid. valid values are: Mumbai, Pune, Delhi
  - selfEmployed 'dddd' is not a valid Boolean
  - age '150' should be <= 100
syntax error at ./test.csv:5
  - 2 value(s) found. expected number of values: 4
validation failed at ./test.csv:6
  - name 'ab cd' did not match regex: [a-zA-Z]*
  - age '2' should be >= 10
```
