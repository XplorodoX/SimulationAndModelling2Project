import os
import time
import pandas as pd
from sqlalchemy import create_engine, text
from sqlalchemy.exc import OperationalError
from typing import Optional, List, Dict, Any

# --- Konfiguration aus Umgebungsvariablen ---
DB_USER = os.getenv("DB_USER", "user")
DB_PASSWORD = os.getenv("DB_PASSWORD", "password")
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "simdata")
MAX_RETRIES = 5
RETRY_DELAY = 5

# --- Konfiguration für CSV-Importe ---
# Dieser Ansatz ist flexibler. Füge einfach ein neues Wörterbuch hinzu,
# um eine weitere CSV-Datei zu importieren.
IMPORTS_CONFIG: List[Dict[str, Any]] = [
    {
        "path": "sample_csv.csv",
        "table_name": "sample_table"
    },
    {
        "path": "household_data_15min_singleindex.csv",
        "table_name": "household_data_15min"
    }
]


def wait_for_db(engine):
    """Wartet, bis die Datenbankverbindung verfügbar ist."""
    print("Warte auf Datenbankverbindung...")
    for i in range(MAX_RETRIES):
        try:
            with engine.connect() as connection:
                connection.execute(text('SELECT 1'))
            print("Datenbankverbindung erfolgreich hergestellt.")
            return True
        except OperationalError:
            print(
                f"Verbindungsversuch {i + 1}/{MAX_RETRIES} fehlgeschlagen. Nächster Versuch in {RETRY_DELAY} Sekunden.")
            time.sleep(RETRY_DELAY)
    print("Fehler: Konnte keine Verbindung zur Datenbank herstellen.")
    return False


def import_csv_to_db(engine, file_path: str, table_name: str):
    """Liest eine CSV-Datei und importiert sie in die Datenbank."""
    print(f"\n--- Importiere '{file_path}' in Tabelle '{table_name}' ---")
    try:
        df = pd.read_csv(file_path)
        print(f"'{file_path}' erfolgreich gelesen.")
        df.to_sql(table_name, engine, if_exists='replace', index=False)
        print(f"Daten erfolgreich in Tabelle '{table_name}' importiert.")
    except FileNotFoundError:
        print(f"Fehler: Die Datei '{file_path}' wurde nicht gefunden.")
    except Exception as e:
        print(f"Ein Fehler beim Importieren der Daten ist aufgetreten: {e}")


def get_current_content(engine, table_name: str) -> Optional[pd.DataFrame]:
    """
    Liest die aktuellen Inhalte aus einer Datenbanktabelle und gibt sie als DataFrame zurück.
    Gibt None zurück, wenn ein Fehler auftritt.
    """
    print(f"\nLese aktuelle Inhalte aus Tabelle '{table_name}'...")
    try:
        df = pd.read_sql(f'SELECT * FROM {table_name}', engine)
        print("Inhalte erfolgreich abgerufen.")
        return df
    except Exception as e:
        print(f"Fehler: Konnte die Inhalte nicht aus der Datenbank lesen. Grund: {e}")
        return None


def read_from_db(engine, table_name: str):
    """Liest Daten aus einer Tabelle und gibt sie direkt auf der Konsole aus."""
    try:
        read_df = pd.read_sql(f'SELECT * FROM {table_name}', engine)
        print(f"\nDaten aus der Datenbank (Tabelle: '{table_name}'):")
        print(read_df.head()) # .head() verwenden, um die Ausgabe bei großen Tabellen zu begrenzen
    except Exception as e:
        print(f"Ein Fehler beim Lesen der Daten ist aufgetreten: {e}")


def main():
    """Hauptfunktion des Skripts."""
    db_url = f'postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}'
    engine = create_engine(db_url)

    if wait_for_db(engine):
        # 1. Alle konfigurierten CSV-Dateien importieren
        for config in IMPORTS_CONFIG:
            import_csv_to_db(engine, config["path"], config["table_name"])

        # 2. Daten aus allen importierten Tabellen zur Überprüfung lesen
        for config in IMPORTS_CONFIG:
            read_from_db(engine, config["table_name"])

        # 3. Beispiel für die Weiterverarbeitung von Daten aus einer Tabelle
        print("\n--- Beispiel für die Weiterverarbeitung ---")
        first_table_name = IMPORTS_CONFIG[0]["table_name"]
        current_content_df = get_current_content(engine, first_table_name)

        if current_content_df is not None:
            print(f"Die abgerufenen Daten aus '{first_table_name}' können nun weiterverarbeitet werden.")
            print(f"Anzahl der Zeilen: {len(current_content_df)}")
            print("Erste Zeile der Daten:")
            print(current_content_df.head(1))


if __name__ == "__main__":
    main()
