# Database Integration Guide

This directory provides a simple Java utility for importing CSV or Excel files
into AnyLogic's built-in database. Table schemas are created automatically from
the file headers, so no manual schema setup is required.

## Prerequisites

* Java (version 8 or later)
* AnyLogic installation (Community or University edition)
* The provided `hsqldb-2.7.4.jar` (only needed when running outside AnyLogic)

## Importing CSV/Excel Files

Compile the helper classes and run `CsvImporter` to create a table from a file:

```bash
javac CsvImporter.java AnyLogicDBUtil.java
java -cp .:hsqldb-2.7.4.jar CsvImporter <tableName> <file.csv> [jdbcUrl]
```

If no `jdbcUrl` is supplied, the importer uses a default in-memory database.
When running inside an AnyLogic model, pass the model's database connection URL
to store the table directly in the model database.

You can also call `AnyLogicDBUtil.importTableFromFile` directly from your own
code. A variant of this method accepts a JDBC URL and handles opening and
closing the connection for you:

```java
AnyLogicDBUtil.importTableFromFile("jdbc:hsqldb:mem:test", "my_table",
        new File("data.csv"));
```

This automatically creates the table and inserts all rows from the CSV file.
