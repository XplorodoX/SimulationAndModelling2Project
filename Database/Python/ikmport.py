import redis
import pandas as pd
from datetime import datetime
import json
import time
from redis.client import Pipeline


class OptimizedTimeSeriesImporter:
    def __init__(self, host='localhost', port=6379, db=0):
        # Connection Pool für bessere Performance
        self.pool = redis.ConnectionPool(
            host=host,
            port=port,
            db=db,
            decode_responses=True,
            max_connections=20,
            socket_connect_timeout=5,
            socket_timeout=5
        )
        self.r = redis.Redis(connection_pool=self.pool)

    def method1_pandas_pipeline(self, csv_file, key_name, batch_size=10000):
        """Schnellste Methode: Pandas + Pipeline + Batch Processing"""
        print("Methode 1: Pandas + Pipeline (Empfohlen)")
        start_time = time.time()

        # CSV mit Pandas lesen (viel schneller als csv.reader)
        df = pd.read_csv(csv_file)

        # Debug: Spalten anzeigen
        print("Gefundene Spalten:", df.columns.tolist())
        print("Spalten nach Strip:", [col.strip() for col in df.columns])

        # Spalten bereinigen (Leerzeichen entfernen)
        df.columns = df.columns.str.strip()

        # Flexible Spaltenerkennung
        time_col = None
        value_col = None

        for col in df.columns:
            if col.lower() in ['time', 'timestamp', 'date', 'datetime']:
                time_col = col
            elif col.lower() in ['kwh', 'value', 'power', 'energy']:
                value_col = col

        if time_col is None or value_col is None:
            print(f"Verfügbare Spalten: {df.columns.tolist()}")
            print("Erste 3 Zeilen:")
            print(df.head(3))
            raise ValueError(f"Konnte Spalten nicht identifizieren. Time: {time_col}, Value: {value_col}")

        print(f"Verwende Spalten - Zeit: '{time_col}', Wert: '{value_col}'")

        df['Time'] = pd.to_datetime(df[time_col])
        df['timestamp'] = (df['Time'].astype('int64') // 10 ** 6).astype(int)  # Millisekunden

        total_rows = len(df)
        print(f"Lade {total_rows} Datenpunkte...")

        # Batch-weise Processing mit Pipeline
        processed = 0
        for i in range(0, total_rows, batch_size):
            batch = df.iloc[i:i + batch_size]

            # Pipeline für Batch-Operationen
            pipe = self.r.pipeline(transaction=False)

            for _, row in batch.iterrows():
                pipe.execute_command('TS.ADD', key_name, row['timestamp'], row[value_col])

            # Alle Commands auf einmal ausführen
            pipe.execute()

            processed += len(batch)
            if processed % (batch_size * 5) == 0:
                print(f"Verarbeitet: {processed}/{total_rows} ({processed / total_rows * 100:.1f}%)")

        end_time = time.time()
        print(f"Fertig! {total_rows} Datenpunkte in {end_time - start_time:.2f} Sekunden")
        print(f"Geschwindigkeit: {total_rows / (end_time - start_time):.0f} Datenpunkte/Sekunde")

    def method2_bulk_insert_sorted_set(self, csv_file, key_name, batch_size=50000):
        """Alternative: Bulk Insert mit Sorted Sets"""
        print("Methode 2: Bulk Insert Sorted Set")
        start_time = time.time()

        df = pd.read_csv(csv_file)

        # Spalten bereinigen
        df.columns = df.columns.str.strip()

        # Flexible Spaltenerkennung
        time_col = None
        value_col = None

        for col in df.columns:
            if col.lower() in ['time', 'timestamp', 'date', 'datetime']:
                time_col = col
            elif col.lower() in ['kwh', 'value', 'power', 'energy']:
                value_col = col

        print(f"Verwende Spalten - Zeit: '{time_col}', Wert: '{value_col}'")

        df['Time'] = pd.to_datetime(df[time_col])
        df['timestamp'] = df['Time'].astype('int64') // 10 ** 9  # Sekunden für Sorted Set

        total_rows = len(df)
        print(f"Lade {total_rows} Datenpunkte...")

        # Bulk Insert in großen Batches
        for i in range(0, total_rows, batch_size):
            batch = df.iloc[i:i + batch_size]

            # Dictionary für ZADD erstellen
            mapping = {}
            for _, row in batch.iterrows():
                data = json.dumps({
                    'time': row['Time'].isoformat(),
                    'kwh': row[value_col]
                })
                mapping[data] = row['timestamp']

            # Bulk Insert
            self.r.zadd(key_name, mapping)

            processed = i + len(batch)
            print(f"Verarbeitet: {processed}/{total_rows} ({processed / total_rows * 100:.1f}%)")

        end_time = time.time()
        print(f"Fertig! {total_rows} Datenpunkte in {end_time - start_time:.2f} Sekunden")
        print(f"Geschwindigkeit: {total_rows / (end_time - start_time):.0f} Datenpunkte/Sekunde")

    def method3_hash_bulk_optimized(self, csv_file, base_key, batch_size=20000):
        """Hash-basiert mit optimiertem Bulk Processing"""
        print("Methode 3: Hash Bulk Processing")
        start_time = time.time()

        df = pd.read_csv(csv_file)

        # Spalten bereinigen
        df.columns = df.columns.str.strip()

        # Flexible Spaltenerkennung
        time_col = None
        value_col = None

        for col in df.columns:
            if col.lower() in ['time', 'timestamp', 'date', 'datetime']:
                time_col = col
            elif col.lower() in ['kwh', 'value', 'power', 'energy']:
                value_col = col

        print(f"Verwende Spalten - Zeit: '{time_col}', Wert: '{value_col}'")

        df['Time'] = pd.to_datetime(df[time_col])
        df['timestamp'] = df['Time'].astype('int64') // 10 ** 9

        total_rows = len(df)
        print(f"Lade {total_rows} Datenpunkte...")

        # Pipeline für Hash-Operations
        pipe = self.r.pipeline(transaction=False)
        index_mapping = {}

        processed = 0
        for _, row in df.iterrows():
            time_str = row['Time'].strftime('%Y-%m-%d_%H:%M:%S')
            hash_key = f"{base_key}:{time_str}"

            # Hash-Felder setzen
            pipe.hset(hash_key, mapping={
                'time': row['Time'].isoformat(),
                'kwh': row[value_col],
                'timestamp': row['timestamp']
            })

            # Für Index sammeln
            index_mapping[hash_key] = row['timestamp']

            processed += 1

            # Batch ausführen
            if processed % batch_size == 0:
                pipe.execute()
                pipe = self.r.pipeline(transaction=False)

                # Index-Update
                self.r.zadd(f"{base_key}:index", index_mapping)
                index_mapping = {}

                print(f"Verarbeitet: {processed}/{total_rows} ({processed / total_rows * 100:.1f}%)")

        # Letzte Batch
        if len(index_mapping) > 0:
            pipe.execute()
            self.r.zadd(f"{base_key}:index", index_mapping)

        end_time = time.time()
        print(f"Fertig! {total_rows} Datenpunkte in {end_time - start_time:.2f} Sekunden")
        print(f"Geschwindigkeit: {total_rows / (end_time - start_time):.0f} Datenpunkte/Sekunde")

    def benchmark_all_methods(self, csv_file):
        """Alle Methoden benchmarken"""
        print("=== BENCHMARK ALLER METHODEN ===\n")

        methods = [
            (self.method1_pandas_pipeline, 'pv_data_pipeline', {}),
            (self.method2_bulk_insert_sorted_set, 'pv_data_sorted', {}),
            (self.method3_hash_bulk_optimized, 'pv_data_hash', {})
        ]

        for method, key, kwargs in methods:
            try:
                # Key löschen falls vorhanden
                self.r.delete(key)
                if 'hash' in key:
                    self.r.delete(f"{key}:index")

                print(f"\n--- {method.__name__} ---")
                method(csv_file, key, **kwargs)

            except Exception as e:
                print(f"Fehler bei {method.__name__}: {e}")

            print("-" * 50)

    # Optimierte Abfrage-Methoden
    def get_data_range_fast(self, key_name, start_time, end_time, method='timeseries'):
        """Schnelle Bereichsabfrage"""
        start_dt = datetime.strptime(start_time, '%Y-%m-%d %H:%M:%S')
        end_dt = datetime.strptime(end_time, '%Y-%m-%d %H:%M:%S')

        if method == 'timeseries':
            start_ts = int(start_dt.timestamp() * 1000)
            end_ts = int(end_dt.timestamp() * 1000)
            return self.r.execute_command('TS.RANGE', key_name, start_ts, end_ts)

        elif method == 'sorted_set':
            start_ts = start_dt.timestamp()
            end_ts = end_dt.timestamp()
            results = self.r.zrangebyscore(key_name, start_ts, end_ts, withscores=True)
            return [(json.loads(data), score) for data, score in results]

    def get_latest_values(self, key_name, count=100, method='timeseries'):
        """Letzte N Werte schnell abrufen"""
        if method == 'timeseries':
            return self.r.execute_command('TS.REVRANGE', key_name, '-', '+', 'COUNT', count)
        elif method == 'sorted_set':
            results = self.r.zrevrange(key_name, 0, count - 1, withscores=True)
            return [(json.loads(data), score) for data, score in results]


# Verwendungsbeispiel
if __name__ == "__main__":
    importer = OptimizedTimeSeriesImporter()

    # Einzelne Methode testen
    importer.method1_pandas_pipeline('PV.csv', 'pv_data_fast')

    # Alle Methoden benchmarken
    #importer.benchmark_all_methods('PV.csv')

    # Schnelle Abfragen testen
    print("\n=== ABFRAGE-TEST ===")
    try:
        # Daten von 2005-01-01 0:00 bis 2:00 abrufen
        result = importer.get_data_range_fast('pv_data_pipeline',
                                              '2005-01-01 00:00:00',
                                              '2005-01-01 02:00:00')
        print(f"Gefunden: {len(result)} Datenpunkte")
        print("Erste 5 Punkte:", result[:5])

        # Letzte 10 Werte
        latest = importer.get_latest_values('pv_data_pipeline', 10)
        print("Letzte 10 Werte:", latest)

    except Exception as e:
        print(f"Abfrage-Fehler: {e}")