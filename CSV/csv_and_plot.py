import csv
from datetime import datetime
from io import StringIO

import matplotlib.dates as mdates
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

COL1 = "Deutschland/Luxemburg [€/MWh] Berechnete Auflösungen"
COL2 = "DE/AT/LU [€/MWh] Berechnete Auflösungen"
EXCLUDE = "∅ Anrainer DE/LU [€/MWh] Berechnete Auflösungen"
ALLOWED = ["Deutschland", "DE", "Luxemburg", "DE/AT/LU"]


def process_price(input_path: str,
                  output_path: str,
                  interval_min: int,
                  write_csv: bool):
    def parse_value(v):
        return 0 if v == '-' else float(v.replace(',', '.'))

    def reformat_date(s):
        return datetime.strptime(s, "%d.%m.%Y %H:%M") \
            .strftime("%Y-%m-%d %H:%M:%S")

    with open(input_path, newline='', encoding='utf-8') as fin:
        reader = csv.reader(fin, delimiter=';')
        header = next(reader)
        header[0] = header[0].lstrip('\ufeff')
        idx_date = header.index("Datum von")
        idx1, idx2 = header.index(COL1), header.index(COL2)

        keep_idx = [idx_date]
        for i, name in enumerate(header):
            if i in (idx_date, idx1, idx2):
                continue
            if any(k in name for k in ALLOWED) and name != EXCLUDE:
                keep_idx.append(i)
        keep_hdr = ["Time"] + [header[i] for i in keep_idx if i != idx_date] + ["price_kWh"]

        rows = []
        for row in reader:
            v1, v2 = parse_value(row[idx1]), parse_value(row[idx2])

            price_mwh = v1 or v2
            price = price_mwh / 1000

            sel = [row[idx_date]] + [row[i] for i in keep_idx if i != idx_date]
            sel[0] = reformat_date(sel[0])
            sel.append(price)
            rows.append(sel)

    df = pd.DataFrame(rows, columns=keep_hdr)
    df['Time'] = pd.to_datetime(df['Time'], format="%Y-%m-%d %H:%M:%S")
    df.set_index('Time', inplace=True)
    df = df[~df.index.duplicated(keep='first')]

    full_idx = pd.date_range(
        start=df.index.min(),
        end=df.index.max(),
        freq=f"{interval_min}min"
    )
    df = df.reindex(full_idx)
    df['price_kWh'] = df['price_kWh'].astype(float).interpolate().bfill().ffill()

    df_out = df.reset_index().rename(columns={'index': 'Time'})
    if write_csv:
        df_out.to_csv(output_path, index=False)
        print(f"Price csv done: {output_path}")
    return df_out


def process_pv(input_path: str,
               output_path: str,
               interval_min: int,
               write_csv: bool):
    with open(input_path, encoding='utf-8') as f:
        lines = [L for L in f if L.strip().startswith("20")]

    df = pd.read_csv(
        StringIO("".join(lines)),
        header=None,
        names=["time", "P", "Gb(i)", "Gd(i)", "Gr(i)", "H_sun", "T2m", "WS10m", "Int"],
        sep=",")[["time", "P"]]

    df["P"] = df["P"].astype(float)
    df["kWh"] = df["P"] / 1000.0
    df["time"] = pd.to_datetime(df["time"], format="%Y%m%d:%H%M")
    df.set_index("time", inplace=True)

    orig_sum = df["kWh"].sum()

    base_idx = pd.date_range(
        start=df.index.min().floor('D'),
        end=df.index.max().ceil('D') - pd.Timedelta(minutes=interval_min),
        freq=f"{interval_min}min"
    )

    df2 = df.reindex(base_idx)
    df2["kWh"] = df2["kWh"].interpolate(method='linear')
    df2["kWh"] = df2["kWh"].fillna(0)

    df2.index = df2.index + pd.Timedelta(hours=1)

    cutoff_date = pd.Timestamp('2023-12-31 23:50:00')
    start_day = df2.index.min().floor('D')
    end_day = min(df2.index.max().ceil('D') - pd.Timedelta(minutes=interval_min), cutoff_date)
    new_index = pd.date_range(start=start_day, end=end_day, freq=f"{interval_min}min")

    df2 = df2.reindex(new_index)
    df2["kWh"] = df2["kWh"].interpolate(method='linear')
    df2["kWh"] = df2["kWh"].fillna(0)

    scale = orig_sum / df2["kWh"].sum()
    df2["kWh"] = scale * df2["kWh"]

    out = df2.reset_index()[["index", "kWh"]]
    out.columns = ["Time", "kWh"]

    if write_csv:
        out.to_csv(output_path, index=False)
        print(f"PV csv done: {output_path}")
    return out


def plot_random(pv_df: pd.DataFrame,
                price_df: pd.DataFrame,
                days_to_plot: int = 3):

    for df in (pv_df, price_df):
        if 'Time' in df.columns:
            df['Time'] = pd.to_datetime(df['Time'], format="%Y-%m-%d %H:%M:%S")
            df.set_index('Time', inplace=True)

    dates_pv = pd.Series(pv_df.index.date).unique()
    dates_pr = pd.Series(price_df.index.date).unique()
    common_days = np.intersect1d(dates_pv, dates_pr)

    days = np.random.choice(common_days, size=min(days_to_plot, len(common_days)), replace=False)
    print("Plot days:", [d.isoformat() for d in days])

    fig, axes = plt.subplots(len(days), 2, figsize=(12, 4 * len(days)), squeeze=False)
    time_fmt = mdates.DateFormatter('%H:%M')

    for i, day in enumerate(days):
        ax1, ax2 = axes[i]

        start = pd.Timestamp(day)
        end = start + pd.Timedelta(days=1)

        d_pv = pv_df.loc[start:end]
        d_pr = price_df.loc[start:end]

        ax1.plot(d_pv.index, d_pv['kWh'], marker='o', label="PV")
        ax1.xaxis.set_major_formatter(time_fmt)
        ax1.set_title(f"PV kWh {day}")
        ax1.set_ylabel("kWh")
        ax1.grid(True)

        ax2.plot(d_pr.index, d_pr['price_kWh'], marker='.', label="Preis")
        ax2.xaxis.set_major_formatter(time_fmt)
        ax2.set_title(f"Price €/kWh {day}")
        ax2.set_ylabel("€/kWh")
        ax2.grid(True)

    plt.tight_layout()
    plt.show()
