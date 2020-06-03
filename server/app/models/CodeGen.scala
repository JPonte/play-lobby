package models

object CodeGen extends App{
  slick.codegen.SourceCodeGenerator.run(
    "slick.jdbc.PostgresProfile",
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5432/lobby?user=ponte&password=123",
    "./server/app",
    "models",
    None,
    None,
    ignoreInvalidDefaults = true,
    outputToMultipleFiles = false
  )
}
