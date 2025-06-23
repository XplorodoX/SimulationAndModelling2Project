import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import hplib as hpl

# 1. Jahreszeitreihe stündlicher Außentemperaturen (Mittelwert 5 °C ±10 K)
dates = pd.date_range('2022-01-01', '2022-12-31 23:00', freq='h')  # 'h' statt 'H' (wegen FutureWarning)
t_amb = 5 + 10 * np.sin(2 * np.pi * (dates.dayofyear - 1) / 365)

# 2. Initialisierung Wärmepumpe und Heizsystem
# Direkte Initialisierung ohne get_parameters
HP = hpl.HeatPump(type='brine/water', group_id=2, t_hs_supply=35)
HS = hpl.HeatingSystem(t_outside_min=-12, t_hs_set=[35, 28], f_hs_exp=1.1)

# 3. Simulation pro Stunde
records = []
for ts, t_out in zip(dates, t_amb):
    # Berechne Sole-Temperatur basierend auf Außentemperatur
    t_brine = HS.calc_brine_temp(t_avg_d=t_out)
    # Berechne COP, elektrische und thermische Leistung
    cop = HP.calc_COP(t_source=t_brine, t_sink=HP.t_hs_supply)
    p_el = HP.calc_electrical_power(cop, load=1.0)  # Vollast
    p_th = cop * p_el

    records.append({
        'time': ts,
        't_out': t_out,
        't_brine': t_brine,
        'COP': cop,
        'P_el': p_el,
        'P_th': p_th
    })

# 4. DataFrame aufbauen
df = pd.DataFrame(records)
df.set_index('time', inplace=True)

# 5. CSV speichern
df.to_csv('simulierte_heizleistung.csv')

# 6. Visualisierung
fig, axes = plt.subplots(3, 1, figsize=(12, 10), sharex=True)
df[['t_out', 't_brine']].plot(ax=axes[0], title='Temperaturen')
df['COP'].plot(ax=axes[1], title='COP')
df[['P_el', 'P_th']].resample('D').sum().plot(ax=axes[2], title='Tägliche Energiemengen [Wh]')
plt.tight_layout()
plt.savefig('wärmepumpe_jahressimulation.png')