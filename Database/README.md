# Database Utilities

Dieses Verzeichnis enthaelt Hilfsprogramme zur Speicherung und Verarbeitung von CSV-Daten in einer PostgreSQL-Datenbank. Die Tools koennen unabhaengig von AnyLogic genutzt werden, um Messreihen in eine Datenbank zu importieren und spaeter wieder auszulesen.

## Struktur

- **DBAbfrage.java** – Java-Programm zum Einlesen von Zeitreihen und einfachen Abfragen.
- **DockerFile/** – `docker-compose.yml` zum Starten einer lokalen PostgreSQL-Datenbank und des Import-Skripts.
- **Python/** – Python-Skripte und Dockerfile fuer den CSV-Import.
- **jar/** – Enthält das PostgreSQL-JDBC-Driver-JAR, das fuer die Java-Programme benoetigt wird.

## Verwendung

1. `docker-compose` im Unterordner `DockerFile` startet eine PostgreSQL-Datenbank und fuehrt das Python-Importskript aus:
   ```bash
   cd Database/DockerFile
   docker-compose up
   ```
2. Das Python-Skript `main.py` importiert die in `IMPORTS_CONFIG` aufgelisteten CSV-Dateien in die Datenbank.
3. `DBAbfrage.java` demonstriert, wie Daten aus der Datenbank abgefragt und verarbeitet werden koennen.

Weitere Details zu den einzelnen Dateien finden sich in den Kommentarblöcken der jeweiligen Quellcodes.

