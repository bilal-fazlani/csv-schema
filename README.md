# CSV Schema

Validate csv files agaists a schema

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