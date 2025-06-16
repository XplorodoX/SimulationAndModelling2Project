import unittest
import os
import pandas as pd
from sqlalchemy import create_engine
from typing import Dict, Any

# Importiere die zu testenden Funktionen aus deinem Skript
from main import import_csv_to_db, get_current_content


class TestDatabaseOperations(unittest.TestCase):

    def setUp(self):
        """
        Diese Methode wird vor jedem einzelnen Test aufgerufen.
        Sie bereitet die Testumgebung vor.
        """
        # Erstelle eine In-Memory-SQLite-Datenbank für den Test.
        # 'sqlite:///:memory:' bedeutet, dass die DB nur im Arbeitsspeicher existiert.
        self.engine = create_engine('sqlite:///:memory:')
        self.test_csv_path = 'test_sample.csv'
        self.table_name = 'test_table'

        # Erstelle ein Test-DataFrame und speichere es als temporäre CSV-Datei.
        self.test_data = pd.DataFrame({
            'id': [1, 2, 3],
            'name': ['Test A', 'Test B', 'Test C'],
            'value': [10.5, 20.0, 30.2]
        })
        self.test_data.to_csv(self.test_csv_path, index=False)

    def tearDown(self):
        """
        Diese Methode wird nach jedem einzelnen Test aufgerufen.
        Sie räumt die Testumgebung auf.
        """
        # Lösche die temporäre CSV-Datei.
        if os.path.exists(self.test_csv_path):
            os.remove(self.test_csv_path)

    def test_import_and_get_content(self):
        """
        Testet den kompletten Prozess: CSV-Import und anschließendes Auslesen.
        """
        # 1. Importiere die Daten aus der Test-CSV in die In-Memory-Datenbank.
        import_csv_to_db(self.engine, self.test_csv_path, self.table_name)

        # 2. Rufe die Daten mit der get_current_content Funktion ab.
        retrieved_df = get_current_content(self.engine, self.table_name)

        # 3. Überprüfe die Ergebnisse mit Assertions.

        # Stelle sicher, dass ein DataFrame zurückgegeben wurde (nicht None).
        self.assertIsNotNone(retrieved_df)

        # Stelle sicher, dass die Anzahl der Zeilen übereinstimmt.
        self.assertEqual(len(self.test_data), len(retrieved_df))

        # Stelle sicher, dass die Spaltennamen identisch sind.
        self.assertListEqual(list(self.test_data.columns), list(retrieved_df.columns))

        # Vergleiche den Inhalt der beiden DataFrames.
        # pd.testing.assert_frame_equal ist ideal für den Vergleich von DataFrames.
        pd.testing.assert_frame_equal(self.test_data, retrieved_df)


if __name__ == '__main__':
    # Führt die Tests aus, wenn die Datei direkt aufgerufen wird.
    unittest.main()
