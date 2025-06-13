import pandas as pd
from io import StringIO


def interpolate_and_format_csv(input_path, output_path, intervall_min=10):
    #Read lines
    with open(input_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    #Keep only non header lines
    dataline_filter = [line for line in lines if line.strip().startswith("20")]

    data = StringIO("".join(dataline_filter))

    #Read CSV with proper header names
    df = pd.read_csv(data, header=None,
                     names=["time", "P", "Gb(i)", "Gd(i)", "Gr(i)", "H_sun", "T2m", "WS10m", "Int"])

    #Keep date and kWh
    df = df[['time', 'P']]
    df['P'] = df['P'].astype(float)

    df['kWh'] = df['P'] / 1000

    #Parse time and set as index
    df['time'] = pd.to_datetime(df['time'], format="%Y%m%d:%H%M")
    df.set_index('time', inplace=True)

    #Define full datetime index with given interval in minutes
    new_index = pd.date_range(
        start=df.index.min().floor('D'),
        end=df.index.max().ceil('D') - pd.Timedelta(minutes=intervall_min),
        freq=f"{intervall_min}min"
    )

    #Reindex and linearly interpolate missing values
    df_interp = df.reindex(new_index)
    df_interp['kWh'] = df_interp['kWh'].interpolate(method='linear')
    df_interp['kWh'].fillna(0, inplace=True)

    original_sum = df['kWh'].sum()

    #Fix timezone issue
    df_interp.index = df_interp.index + pd.Timedelta(hours=1)

    #Limit date to 31 Dec 2023 23:50
    cutoff_date = pd.Timestamp('2023-12-31 23:50:00')
    start_day = df_interp.index.min().floor('D')
    end_day = min(df_interp.index.max().ceil('D') - pd.Timedelta(minutes=intervall_min), cutoff_date)
    new_index_shifted = pd.date_range(start=start_day, end=end_day, freq=f"{intervall_min}min")

    # Final reindex and interpolation after shift
    df_interp = df_interp.reindex(new_index_shifted)
    df_interp['kWh'] = df_interp['kWh'].interpolate(method='linear')
    df_interp['kWh'].fillna(0, inplace=True)

    # Scale interpolated values to original total energy
    interpolated_sum = df_interp['kWh'].sum()
    scale_factor = original_sum / interpolated_sum if interpolated_sum != 0 else 0
    df_interp['kWh'] *= scale_factor

    # Format time column as H:M string
    df_interp_reset = df_interp.reset_index()
    df_interp_reset['time'] = df_interp_reset['index'].dt.strftime('%-H:%M')

    # Export to CSV with header
    df_interp_reset[['time', 'kWh']].to_csv(output_path, header=['Time', 'kWh'], index=False)
