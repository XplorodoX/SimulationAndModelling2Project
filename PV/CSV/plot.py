import pandas as pd
import matplotlib.pyplot as plt
import numpy as np


def plot(output_path):
    df = pd.read_csv(output_path)

    df['Time'] = pd.to_datetime(df['Time'])

    df.set_index('Time', inplace=True)

    unique_dates = np.unique(df.index.date)

    random_days = np.random.choice(unique_dates, size=3, replace=False)
    print("Selected days to plot:", random_days)

    plt.figure(figsize=(15, 10))

    for i, day in enumerate(random_days, start=1):
        day_data = df.loc[str(day)]
        plt.subplot(3, 1, i)
        plt.plot(day_data.index, day_data['kWh'], marker='o', linestyle='-')
        plt.title(f"Interpolated kWh for {day}")
        plt.xlabel("Time")
        plt.ylabel("kWh")
        plt.grid(True)

    plt.tight_layout()
    plt.show()
