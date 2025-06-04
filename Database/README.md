# Database Integration Guide

This directory contains a small Java example showing how to store simulation data into a relational database. The same approach can be used inside an AnyLogic model.

## Prerequisites

* Java (version 8 or later)
* [HSQLDB](https://hsqldb.org/) – the `hsqldb-2.7.4.jar` is already included
* AnyLogic installation (Community or University edition)

## Starting the HSQLDB Server

1. From this folder start the HSQLDB server:
   ```bash
   java -cp hsqldb-2.7.4.jar org.hsqldb.server.Server \
       --database.0 file:./database/db --dbname.0 test
   ```
   This starts a local database named `test` that listens on port `9001`.

## Running the Example

Compile and run `DatabaseTest` to create the schema and insert a sample record:

```bash
javac *.java
java -cp .:hsqldb-2.7.4.jar DatabaseTest
```

If everything works you will see `Test passed` in the console.

## Connecting from AnyLogic

1. Copy `hsqldb-2.7.4.jar` into the `<AnyLogic>/lib/` directory or add it to the model's classpath.
2. In AnyLogic, add a `Database` element to your model.
3. In the Database properties set the connection URL to:
   ```
   jdbc:hsqldb:hsql://localhost:9001/test
   ```
   Use username `SA` with an empty password.
4. You can then access the database through AnyLogic's built‑in blocks or via custom Java code (see `AnyLogicDBUtil`).

This setup allows you to persist simulation outputs and later analyse them with SQL tools.
