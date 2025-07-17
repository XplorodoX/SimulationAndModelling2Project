import pandas as pd
import os
import numpy as np
from datetime import datetime, timedelta


def write_dummy_csv(filename="raw_data.csv"):
    """
    Write the provided data to a CSV file in the current folder.

    Parameters:
    - data: list of dicts or pandas DataFrame
    - filename: name of the CSV file to save (default: raw_data.csv)
    """
    
    # Example usage
    # Generate sample data: timestamps are integers starting from 0, values are uniform between 0 and 1
    np.random.seed(0)
    num_samples = 20000
    data = [
        {"timestamp": i, "value": float(np.random.uniform(0, 1))}
        for i in range(num_samples)
    ]
    
    if not isinstance(data, pd.DataFrame):
        df = pd.DataFrame(data)
    else:
        df = data
    # Ensure the header is ['timestamp', 'value']
    df = df[['timestamp', 'value']]
    filepath = os.path.join(os.path.dirname(__file__), filename)
    try:
        df.to_csv(filepath, index=False)
        print(f"CSV file saved at: {filepath}")
    except Exception as e:
        print("Failed to write CSV file.")
        print(f"Error: {e}")

def convert_raw_to_usable_csv(
    input_filename="raw_data.csv", 
    output_filename="heatpumpDataFromModel.csv",
    valueMultiplier = 1 # valueMultiplier is used that the output data is in kWh, even if inputdata isnt
):
    """
    Reads the dummy data from input_filename and saves it to output_filename,
    expanding each hourly value into 4 rows at 15-minute intervals (same value for all 4).
    The timestamp is converted into a string formatted like a Java LocalDateTime (e.g., '2024-01-01T13:15:00').
    """

    # You can set the base date here
    base_date = datetime(2016, 1, 1, 0, 0, 0)

    input_path = os.path.join(os.path.dirname(__file__), input_filename)
    output_path = os.path.join(os.path.dirname(__file__), output_filename)
    try:
        df = pd.read_csv(input_path)
        expanded_rows = []
        for _, row in df.iterrows():
            hour = int(row['timestamp'])
            value = row['value'] * valueMultiplier
            for quarter in range(4):
                # 0.00, 0.25, 0.50, 0.75 hours
                timestamp = hour + 0.25 * quarter
                # Calculate the datetime
                dt = base_date + timedelta(hours=timestamp)
                # Format as Java LocalDateTime string
                # timestamp_str = dt.strftime("%Y-%m-%dT%H:%M:%S")  # Old Java LocalDateTime format
                timestamp_str = dt.strftime("%Y-%m-%d %H:%M:%S")
                expanded_rows.append({'timestamp': timestamp_str, 'value': value})
        expanded_df = pd.DataFrame(expanded_rows)
        expanded_df.to_csv(output_path, index=False)
        print(f"Converted data saved at: {output_path}")
    except Exception as e:
        print("Failed to convert and write CSV file.")
        print(f"Error: {e}")

if __name__ == "__main__":
    
    #write_dummy_csv(filename="raw_data.csv")
    convert_raw_to_usable_csv(input_filename="time_series_heat_pump.csv", output_filename="heatpump.csv", valueMultiplier=1e-3)
