# Database Utilities

This directory contains helper utilities for storing and processing CSV data in a PostgreSQL database. The tools can be used independently of AnyLogic to import measurement series into a database and retrieve them later on.

## Structure

- **DBRequest.java** – Java program for reading time series data and performing simple queries.
- **DockerFile/** – `docker-compose.yml` to start a local PostgreSQL database and run the import script.
- **Python/** – Python scripts and Dockerfile used for CSV import.
- **jar/** – Contains the PostgreSQL JDBC driver JAR required for the Java programs.

## Usage

1. `docker-compose` in the `DockerFile` subfolder starts a PostgreSQL database and runs the Python import script:
   ```bash
   cd Database/DockerFile
   docker-compose up
   ```
2. The Python script `main.py` imports the CSV files listed in `IMPORTS_CONFIG` into the database.
3. `DBRequest.java` demonstrates how data can be queried from the database and further processed.

Further details about the individual files can be found in the comment blocks of the respective source codes.

