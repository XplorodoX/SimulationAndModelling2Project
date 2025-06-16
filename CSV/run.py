from csv_and_plot import process_price, process_pv, plot_random

interval_min = 15

write_csv = True

df_pv = process_pv("PVRawData.csv", "PV.csv", interval_min = interval_min, write_csv = write_csv)

df_price = process_price("electricityPricesRawData.csv", "price.csv", interval_min = interval_min, write_csv = write_csv)

plot_random(df_pv, df_price, days_to_plot=3)


