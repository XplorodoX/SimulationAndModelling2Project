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

You can also call `AnyLogicDBUtil.importTableFromFile` from your own code to
perform the same operation programmatically.
