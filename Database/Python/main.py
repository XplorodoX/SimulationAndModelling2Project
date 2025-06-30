"""
Helper script for importing CSV files into a PostgreSQL database.
It is started automatically in the Docker environment but can also be run
standalone.
"""

import os
import time
import pandas as pd
from sqlalchemy import create_engine, text
from sqlalchemy.exc import OperationalError
from typing import Optional, List, Dict, Any

# --- Configuration from environment variables ---
DB_USER = os.getenv("DB_USER", "user")
DB_PASSWORD = os.getenv("DB_PASSWORD", "password")
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = os.getenv("DB_PORT", "5432")
DB_NAME = os.getenv("DB_NAME", "simdata")
MAX_RETRIES = 5
RETRY_DELAY = 5

# --- Configuration for CSV imports ---
# Added a key "timestamp_columns" listing the timestamp columns.
IMPORTS_CONFIG: List[Dict[str, Any]] = [
    {
        "path": "PV.csv",
        "table_name": "pv",
        "timestamp_columns": ["time"]
    },
    {
        "path": "household_data_15min_singleindex.csv",
        "table_name": "household_data",
        "timestamp_columns": ["date"]
    },
    {
        "path": "price.csv",
        "table_name": "price",
        "timestamp_columns": ["time"]
    },
    {
        "path": "heatpump.csv",
        "table_name": "heatpump",
        "timestamp_columns": ["time"]
    }
]


def wait_for_db(engine):
    """Wait until the database connection is available."""
    print("Waiting for database connection...")
    for i in range(MAX_RETRIES):
        try:
            with engine.connect() as connection:
                connection.execute(text('SELECT 1'))
            print("Database connection established.")
            return True
        except OperationalError:
            print(
                f"Connection attempt {i + 1}/{MAX_RETRIES} failed. Next try in {RETRY_DELAY} seconds.")
            time.sleep(RETRY_DELAY)
    print("Error: Could not establish a connection to the database.")
    return False


# MODIFIED: The function accepts an optional list of timestamp columns.
def import_csv_to_db(engine, file_path: str, table_name: str, timestamp_columns: Optional[List[str]] = None):
    """
    Read a CSV file, convert timestamp columns and import it into the database.
    """
    print(f"\n--- Importing '{file_path}' into table '{table_name}' ---")
    try:
        # Columns listed in 'timestamp_columns' are parsed directly as datetime.
        df = pd.read_csv(file_path, parse_dates=timestamp_columns)
        print(f"'{file_path}' read successfully.")

        # Optional: check the data types before import
        print("DataFrame info (dtypes):")
        df.info()

        df.to_sql(table_name, engine, if_exists='replace', index=False)
        print(f"Data successfully imported into table '{table_name}'.")

    except FileNotFoundError:
        print(f"Error: File '{file_path}' not found.")
    except Exception as e:
        print(f"An error occurred while importing the data: {e}")


def get_current_content(engine, table_name: str) -> Optional[pd.DataFrame]:
    """
    Read the current contents of a table and return them as a DataFrame.
    Returns None if an error occurs.
    """
    print(f"\nReading current contents from table '{table_name}'...")
    try:
        df = pd.read_sql(f'SELECT * FROM {table_name}', engine)
        print("Contents retrieved successfully.")
        return df
    except Exception as e:
        print(f"Error: Could not read contents from the database. Reason: {e}")
        return None


def read_from_db(engine, table_name: str):
    """Read data from a table and print it directly to the console."""
    try:
        read_df = pd.read_sql(f'SELECT * FROM {table_name}', engine)
        print(f"\nData from the database (table: '{table_name}'):")
        # .head() keeps the output short for large tables
        print(read_df.head())
    except Exception as e:
        print(f"An error occurred while reading the data: {e}")


def main():
    """Main entry point of the script."""
    db_url = f'postgresql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}'
    engine = create_engine(db_url)

    if wait_for_db(engine):
        # 1. Import all configured CSV files
        for config in IMPORTS_CONFIG:
            import_csv_to_db(engine, config["path"], config["table_name"])

        # 2. Read data from all imported tables for verification
        for config in IMPORTS_CONFIG:
            read_from_db(engine, config["table_name"])

        # 3. Example for further processing of data from a table
        print("\n--- Example for further processing ---")
        first_table_name = IMPORTS_CONFIG[0]["table_name"]
        current_content_df = get_current_content(engine, first_table_name)

        if current_content_df is not None:
            print(f"The retrieved data from '{first_table_name}' can now be processed further.")
            print(f"Number of rows: {len(current_content_df)}")
            print("First row of data:")
            print(current_content_df.head(1))


if __name__ == "__main__":
    main()
