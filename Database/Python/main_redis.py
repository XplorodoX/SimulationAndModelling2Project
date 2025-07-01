"""
Helper script for importing CSV files into a Redis database.
It is started automatically in the Docker environment but can also be run
standalone.
"""

import os
import time
import pandas as pd
import redis
from typing import Optional, List, Dict, Any

# --- Configuration from environment variables ---
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = os.getenv("REDIS_PORT", "6379")
REDIS_PASSWORD = os.getenv("REDIS_PASSWORD", "")
REDIS_DB = int(os.getenv("REDIS_DB", "0"))
MAX_RETRIES = 5
RETRY_DELAY = 5

# --- Configuration for CSV imports ---
IMPORTS_CONFIG: List[Dict[str, Any]] = [
    {
        "path": "PV.csv",
        "table_name": "pv",
        "timestamp_columns": ["time"]
    }
]


def wait_for_db(redis_client: redis.Redis) -> bool:
    """Wait until the Redis connection is available."""
    print("Waiting for Redis connection...")
    for i in range(MAX_RETRIES):
        try:
            redis_client.ping()
            print("Redis connection established.")
            return True
        except redis.ConnectionError:
            print(
                f"Connection attempt {i + 1}/{MAX_RETRIES} failed. Next try in {RETRY_DELAY} seconds.")
            time.sleep(RETRY_DELAY)
    print("Error: Could not establish a connection to Redis.")
    return False


def import_csv_to_db(redis_client: redis.Redis, file_path: str, table_name: str, timestamp_columns: Optional[List[str]] = None, timeseries_column: Optional[str] = None, value_column: Optional[str] = None):
    """
    Liest eine CSV-Datei und importiert sie effizient in Redis.
    Wenn 'timeseries_column' und 'value_column' angegeben sind, werden die Daten
    als Sorted Set für Zeitreihenabfragen importiert. Andernfalls als Hashes.
    """
    print(f"\n--- Importing '{file_path}' into Redis table '{table_name}' ---")
    try:
        df = pd.read_csv(file_path)
        print(f"'{file_path}' read successfully. DataFrame has {len(df)} rows.")

        pipe = redis_client.pipeline()

        # Zeitreihen-Import mit Sorted Sets (ZADD)
        if timeseries_column and value_column and timeseries_column in df.columns and value_column in df.columns:
            print(f"Importing as timeseries into Sorted Set '{table_name}'.")
            df[timeseries_column] = pd.to_datetime(df[timeseries_column])

            zadd_data = {}
            for _, row in df.iterrows():
                timestamp = int(row[timeseries_column].timestamp())
                # Member darf nicht leer sein, sonst Fehler.
                # Wir speichern den Preis als Member und den Zeitstempel als Score.
                # Um Duplikate zu vermeiden (gleicher Preis zur gleichen Zeit),
                # fügen wir den Zeitstempel zum Member hinzu.
                member = f"{row[value_column]}:{timestamp}"
                zadd_data[member] = timestamp

            if zadd_data:
                # Der Schlüssel für das Sorted Set ist der Tabellenname selbst.
                pipe.zadd(table_name, zadd_data)

        # Standard-Import mit Hashes (HSET)
        else:
            print(f"Importing as Hashes with index set '{table_name}:keys'.")
            if timestamp_columns:
                for ts_col in timestamp_columns:
                    if ts_col in df.columns:
                        df[ts_col] = df[ts_col].astype(str)

            records = df.to_dict(orient='records')
            keys_to_add = []
            for index, record in enumerate(records):
                key = f"{table_name}:{index}"
                filtered_record = {k: v for k, v in record.items() if pd.notna(v)}
                if filtered_record: # Nur importieren, wenn Daten vorhanden sind
                    pipe.hset(key, mapping=filtered_record)
                    keys_to_add.append(key)

            if keys_to_add:
                pipe.sadd(f"{table_name}:keys", *keys_to_add)

            if not df.empty:
                pipe.sadd(f"{table_name}:columns", *df.columns.tolist())

        pipe.execute()
        print(f"Data successfully imported into Redis table '{table_name}'.")

    except FileNotFoundError:
        print(f"Error: File '{file_path}' not found.")
    except Exception as e:
        print(f"An error occurred while importing the data: {e}")


def get_current_content(redis_client: redis.Redis, table_name: str) -> Optional[pd.DataFrame]:
    """
    Read the current contents from Redis and return them as a DataFrame.
    This function is for data stored as Hashes, not for Sorted Sets.
    """
    print(f"\nReading current contents from Redis table '{table_name}'...")
    try:
        key_type = redis_client.type(f"{table_name}:keys")
        if key_type != 'set':
            print(f"Cannot read '{table_name}'. The key '{table_name}:keys' is of type '{key_type}', not 'set'.")
            return None

        keys = redis_client.smembers(f"{table_name}:keys")
        if not keys:
            print(f"No data found for '{table_name}'.")
            return pd.DataFrame()

        pipe = redis_client.pipeline()
        for key in keys:
            pipe.hgetall(key)

        results = pipe.execute()

        # Filtere leere Dictionaries heraus, die von leeren Hashes stammen könnten
        valid_results = [res for res in results if res]
        if not valid_results:
            return pd.DataFrame()

        df = pd.DataFrame(valid_results)
        print("Contents retrieved successfully.")
        return df

    except Exception as e:
        print(f"Error: Could not read contents from Redis. Reason: {e}")
        return None


def read_from_db(redis_client: redis.Redis, table_name: str):
    """Read data from Redis and print it directly to the console."""
    df = get_current_content(redis_client, table_name)
    if df is not None:
        if not df.empty:
            print(f"\nData from Redis (table: '{table_name}'):")
            print(df.head())
        else:
            print(f"No data to display for table '{table_name}'.")


def main():
    """Main entry point of the script."""
    redis_client = redis.Redis(
        host=REDIS_HOST,
        port=int(REDIS_PORT),
        password=REDIS_PASSWORD,
        db=REDIS_DB,
        decode_responses=True
    )

    if wait_for_db(redis_client):
        # Optional: Datenbank vor dem Import leeren, um sauberen Zustand zu gewährleisten
        # redis_client.flushdb()
        # print("\nRedis database flushed.")

        for config in IMPORTS_CONFIG:
            import_csv_to_db(
                redis_client,
                config["path"],
                config["table_name"],
                config.get("timestamp_columns"),
                config.get("timeseries_column"),
                config.get("value_column")
            )

        for config in IMPORTS_CONFIG:
            if "timeseries_column" not in config:
                read_from_db(redis_client, config["table_name"])


if __name__ == "__main__":
    main()