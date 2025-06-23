# Python

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import hplib as hpl  # angenommenes Modul

# 1. Jahreszeitreihe stündlicher Außentemperaturen
dates = pd.date_range('2022-01-01','2022-12-31 23:00', freq='H')
# Beispiel: Mittelwert 5 °C + jährliche Sinus-Schwankung ±10 K
t_outside = 5 + 10 * np.sin(2*np.pi*(dates.dayofyear-1)/365)

# 2. Initialisierung Wärmepumpe und Heizsystem
HP = hpl.HeatPump(type='brine/water', group_id=2, t_hs_supply=35)
HS = hpl.HeatingSystem(t_outside_min=-11, t_hs_set=[35,28], f_hs_exp=1.1)

# 3. Simulation pro Stunde
records = []
for ts, t_out in zip(dates, t_outside):
    t_brine = HS.calc_brine_temp(t_avg_d=t_out)
    cop      = HP.calc_COP(t_source=t_brine, t_sink=HP.t_hs_supply)
    p_el     = HP.calc_electrical_power(cop, load=1.0)      # load=1.0 als Nennlast
    p_th     = cop * p_el
    records.append((ts, t_out, t_brine, cop, p_el, p_th))

# 4. DataFrame aufbauen
df_year = pd.DataFrame(records, columns=[
    'time', 't_out', 't_brine', 'COP', 'P_el', 'P_th'
]).set_index('time')

# 5. Visualisierung
fig, axes = plt.subplots(3,1, figsize=(12,10), sharex=True)
df_year[['t_out','t_brine']].plot(ax=axes[0], title='Temperaturen')
df_year['COP'].rolling(24).mean().plot(ax=axes[1], title='COP (24 h Mittel)')
df_year[['P_el','P_th']].resample('D').sum().plot(ax=axes[2], title='Tägliche Energiemengen [Wh]')
plt.tight_layout()

if __name__ == '__main__':
    plt.show()
    # Optional: Speichern des DataFrames als CSV
    df_year.to_csv('heat_pump_simulation.csv')
    print("Simulationsergebnisse gespeichert in 'heat_pump_simulation.csv'.")