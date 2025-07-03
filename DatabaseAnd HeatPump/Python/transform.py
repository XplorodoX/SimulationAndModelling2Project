"""
Utility for preprocessing CSV files before they are imported.
As an example a timestamp column is adjusted and unnecessary columns are removed.
"""

import pandas as pd

def convert_timestamp(df: pd.DataFrame, column_name: str = 'utc_timestamp') -> pd.DataFrame:
    """
    Convert the format of the given column and add 15 minutes.
    """
    df[column_name] = pd.to_datetime(df[column_name], utc=True)
    df[column_name] = df[column_name] + pd.Timedelta(minutes=15)
    df[column_name] = df[column_name].dt.strftime('%Y-%m-%d %H:%M:%S')
    return df

def remove_column(df: pd.DataFrame, column_name: str) -> pd.DataFrame:
    """
    Remove the specified column from the DataFrame.
    """
    return df.drop(columns=[column_name])

if __name__ == '__main__':
    input_path = 'oldi/household_data_15min_singleindex.csv'
    output_path = 'household_data.csv'
    timestamp_column = 'utc_timestamp'
    column_to_remove = 'cet_cest_timestamp'

    df = pd.read_csv(input_path)
    df = convert_timestamp(df, timestamp_column)
    if column_to_remove in df.columns:
        df = remove_column(df, column_to_remove)
    df.to_csv(output_path, index=False)
