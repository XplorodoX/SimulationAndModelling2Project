import redis
import pandas as pd
from datetime import datetime, timedelta
import time
from typing import List, Tuple, Optional, Union
import numpy as np


class OptimizedTimeSeriesLoader:
    def __init__(self, host='localhost', port=6379, db=0):
        """Optimierte Redis-Verbindung für TimeSeries-Daten"""
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

    def import_csv_optimized(self, csv_file: str, key_name: str, batch_size: int = 10000) -> dict:
        """
        Optimierter CSV-Import basierend auf der schnellsten Methode (Pandas + Pipeline)

        Args:
            csv_file: Pfad zur CSV-Datei
            key_name: Redis-Key für die TimeSeries
            batch_size: Anzahl Datenpunkte pro Batch

        Returns:
            dict: Import-Statistiken
        """
        print(f"Importiere CSV: {csv_file} -> Redis Key: {key_name}")
        start_time = time.time()

        # CSV mit Pandas lesen
        df = pd.read_csv(csv_file)
        df.columns = df.columns.str.strip()

        # Flexible Spaltenerkennung
        time_col = self._detect_time_column(df)
        value_col = self._detect_value_column(df)

        print(f"Verwende Spalten - Zeit: '{time_col}', Wert: '{value_col}'")

        # Zeitstempel konvertieren
        df['Time'] = pd.to_datetime(df[time_col])
        df['timestamp'] = (df['Time'].astype('int64') // 10 ** 6).astype(int)  # Millisekunden

        total_rows = len(df)
        print(f"Importiere {total_rows} Datenpunkte...")

        # TimeSeries erstellen falls nicht vorhanden
        try:
            self.r.execute_command('TS.CREATE', key_name)
        except:
            pass  # Key existiert bereits

        # Batch-Processing mit Pipeline
        processed = 0
        for i in range(0, total_rows, batch_size):
            batch = df.iloc[i:i + batch_size]

            pipe = self.r.pipeline(transaction=False)
            for _, row in batch.iterrows():
                pipe.execute_command('TS.ADD', key_name, row['timestamp'], row[value_col])

            pipe.execute()
            processed += len(batch)

            if processed % (batch_size * 5) == 0:
                print(f"Verarbeitet: {processed}/{total_rows} ({processed / total_rows * 100:.1f}%)")

        end_time = time.time()
        duration = end_time - start_time
        speed = total_rows / duration

        stats = {
            'total_rows': total_rows,
            'duration_seconds': duration,
            'speed_per_second': speed,
            'key_name': key_name
        }

        print(f"Import abgeschlossen! {total_rows} Datenpunkte in {duration:.2f}s")
        print(f"Geschwindigkeit: {speed:.0f} Datenpunkte/Sekunde")

        return stats

    def get_data_by_date(self, key_name: str, date: str,
                         time_start: str = "00:00:00",
                         time_end: str = "23:59:59") -> pd.DataFrame:
        """
        Lade alle Daten für einen bestimmten Tag

        Args:
            key_name: Redis TimeSeries Key
            date: Datum im Format 'YYYY-MM-DD'
            time_start: Startzeit (Standard: 00:00:00)
            time_end: Endzeit (Standard: 23:59:59)

        Returns:
            pd.DataFrame: Daten als DataFrame
        """
        start_dt = datetime.strptime(f"{date} {time_start}", '%Y-%m-%d %H:%M:%S')
        end_dt = datetime.strptime(f"{date} {time_end}", '%Y-%m-%d %H:%M:%S')

        return self._get_data_range(key_name, start_dt, end_dt)

    def get_data_by_datetime_range(self, key_name: str,
                                   start_datetime: str,
                                   end_datetime: str) -> pd.DataFrame:
        """
        Lade Daten für einen bestimmten Zeitbereich

        Args:
            key_name: Redis TimeSeries Key
            start_datetime: Start im Format 'YYYY-MM-DD HH:MM:SS'
            end_datetime: Ende im Format 'YYYY-MM-DD HH:MM:SS'

        Returns:
            pd.DataFrame: Daten als DataFrame
        """
        start_dt = datetime.strptime(start_datetime, '%Y-%m-%d %H:%M:%S')
        end_dt = datetime.strptime(end_datetime, '%Y-%m-%d %H:%M:%S')

        return self._get_data_range(key_name, start_dt, end_dt)

    def get_data_last_hours(self, key_name: str, hours: int) -> pd.DataFrame:
        """
        Lade Daten der letzten N Stunden

        Args:
            key_name: Redis TimeSeries Key
            hours: Anzahl Stunden zurück

        Returns:
            pd.DataFrame: Daten als DataFrame
        """
        end_dt = datetime.now()
        start_dt = end_dt - timedelta(hours=hours)

        return self._get_data_range(key_name, start_dt, end_dt)

    def get_data_last_days(self, key_name: str, days: int) -> pd.DataFrame:
        """
        Lade Daten der letzten N Tage

        Args:
            key_name: Redis TimeSeries Key
            days: Anzahl Tage zurück

        Returns:
            pd.DataFrame: Daten als DataFrame
        """
        end_dt = datetime.now()
        start_dt = end_dt - timedelta(days=days)

        return self._get_data_range(key_name, start_dt, end_dt)

    def get_latest_values(self, key_name: str, count: int = 100) -> pd.DataFrame:
        """
        Lade die neuesten N Werte

        Args:
            key_name: Redis TimeSeries Key
            count: Anzahl der Werte

        Returns:
            pd.DataFrame: Neueste Daten als DataFrame
        """
        try:
            result = self.r.execute_command('TS.REVRANGE', key_name, '-', '+', 'COUNT', count)

            if not result:
                return pd.DataFrame()

            # Daten zu DataFrame konvertieren
            timestamps = [int(item[0]) for item in result]
            values = [float(item[1]) for item in result]

            df = pd.DataFrame({
                'timestamp': timestamps,
                'value': values
            })

            # Timestamps zu DateTime konvertieren
            df['datetime'] = pd.to_datetime(df['timestamp'], unit='ms')
            df = df.sort_values('datetime').reset_index(drop=True)

            return df[['datetime', 'value']]

        except Exception as e:
            print(f"Fehler beim Laden der neuesten Werte: {e}")
            return pd.DataFrame()

    def _get_data_range(self, key_name: str, start_dt: datetime, end_dt: datetime) -> pd.DataFrame:
        """Interne Methode für Bereichsabfragen"""
        try:
            start_ts = int(start_dt.timestamp() * 1000)  # Millisekunden
            end_ts = int(end_dt.timestamp() * 1000)

            print(f"Lade Daten von {start_dt} bis {end_dt}")

            result = self.r.execute_command('TS.RANGE', key_name, start_ts, end_ts)

            if not result:
                print("Keine Daten im angegebenen Zeitraum gefunden")
                return pd.DataFrame()

            # Daten zu DataFrame konvertieren
            timestamps = [int(item[0]) for item in result]
            values = [float(item[1]) for item in result]

            df = pd.DataFrame({
                'timestamp': timestamps,
                'value': values
            })

            # Timestamps zu DateTime konvertieren
            df['datetime'] = pd.to_datetime(df['timestamp'], unit='ms')

            print(f"Gefunden: {len(df)} Datenpunkte")
            return df[['datetime', 'value']]

        except Exception as e:
            print(f"Fehler beim Laden der Daten: {e}")
            return pd.DataFrame()

    def get_aggregated_data(self, key_name: str, start_datetime: str, end_datetime: str,
                            aggregation: str = 'avg', bucket_size: int = 3600000) -> pd.DataFrame:
        """
        Lade aggregierte Daten (Durchschnitt, Summe, etc.)

        Args:
            key_name: Redis TimeSeries Key
            start_datetime: Start im Format 'YYYY-MM-DD HH:MM:SS'
            end_datetime: Ende im Format 'YYYY-MM-DD HH:MM:SS'
            aggregation: Art der Aggregation ('avg', 'sum', 'min', 'max', 'count')
            bucket_size: Bucket-Größe in Millisekunden (Standard: 1 Stunde)

        Returns:
            pd.DataFrame: Aggregierte Daten
        """
        try:
            start_dt = datetime.strptime(start_datetime, '%Y-%m-%d %H:%M:%S')
            end_dt = datetime.strptime(end_datetime, '%Y-%m-%d %H:%M:%S')

            start_ts = int(start_dt.timestamp() * 1000)
            end_ts = int(end_dt.timestamp() * 1000)

            result = self.r.execute_command('TS.RANGE', key_name, start_ts, end_ts,
                                            'AGGREGATION', aggregation, bucket_size)

            if not result:
                return pd.DataFrame()

            timestamps = [int(item[0]) for item in result]
            values = [float(item[1]) for item in result]

            df = pd.DataFrame({
                'timestamp': timestamps,
                f'{aggregation}_value': values
            })

            df['datetime'] = pd.to_datetime(df['timestamp'], unit='ms')

            return df[['datetime', f'{aggregation}_value']]

        except Exception as e:
            print(f"Fehler bei aggregierten Daten: {e}")
            return pd.DataFrame()

    def _detect_time_column(self, df: pd.DataFrame) -> str:
        """Automatische Erkennung der Zeitspalte"""
        for col in df.columns:
            if col.lower() in ['time', 'timestamp', 'date', 'datetime']:
                return col
        raise ValueError("Keine Zeitspalte gefunden")

    def _detect_value_column(self, df: pd.DataFrame) -> str:
        """Automatische Erkennung der Wertspalte"""
        for col in df.columns:
            if col.lower() in ['kwh', 'value', 'power', 'energy', 'wert']:
                return col
        raise ValueError("Keine Wertspalte gefunden")

    def get_info(self, key_name: str) -> dict:
        """Informationen über die TimeSeries"""
        try:
            info = self.r.execute_command('TS.INFO', key_name)
            return dict(zip(info[::2], info[1::2]))
        except Exception as e:
            print(f"Fehler beim Abrufen der Info: {e}")
            return {}


# Verwendungsbeispiele
if __name__ == "__main__":
    loader = OptimizedTimeSeriesLoader()

    # 1. CSV importieren (nur einmal nötig)
    # stats = loader.import_csv_optimized('PV.csv', 'pv_data_optimized')

    # 2. Daten für einen bestimmten Tag laden
    print("=== DATEN FÜR EINEN TAG ===")
    day_data = loader.get_data_by_date('pv_data_pipeline', '2005-01-01')
    print(f"Daten für 2005-01-01: {len(day_data)} Datenpunkte")
    if not day_data.empty:
        print(day_data.head())

    # 3. Daten für einen Zeitbereich laden
    print("\n=== DATEN FÜR ZEITBEREICH ===")
    range_data = loader.get_data_by_datetime_range(
        'pv_data_pipeline',
        '2005-01-01 08:00:00',
        '2005-01-01 18:00:00'
    )
    print(f"Daten 08:00-18:00: {len(range_data)} Datenpunkte")

    # 4. Letzte 24 Stunden
    print("\n=== LETZTE 24 STUNDEN ===")
    recent_data = loader.get_data_last_hours('pv_data_pipeline', 24)
    print(f"Letzte 24h: {len(recent_data)} Datenpunkte")

    # 5. Neueste 100 Werte
    print("\n=== NEUESTE 100 WERTE ===")
    latest = loader.get_latest_values('pv_data_pipeline', 100)
    print(f"Neueste Werte: {len(latest)} Datenpunkte")
    if not latest.empty:
        print(latest.tail())

    # 6. Aggregierte Daten (Stündliche Durchschnitte)
    print("\n=== AGGREGIERTE DATEN ===")
    hourly_avg = loader.get_aggregated_data(
        'pv_data_pipeline',
        '2005-01-01 00:00:00',
        '2005-01-02 00:00:00',
        aggregation='avg',
        bucket_size=3600000  # 1 Stunde in Millisekunden
    )
    print(f"Stündliche Durchschnitte: {len(hourly_avg)} Datenpunkte")

    # 7. TimeSeries Info
    print("\n=== TIMESERIES INFO ===")
    info = loader.get_info('pv_data_pipeline')
    for key, value in info.items():
        print(f"{key}: {value}")