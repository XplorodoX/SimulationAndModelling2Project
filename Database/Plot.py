import pandas as pd
import plotly.graph_objects as go
from plotly.subplots import make_subplots
import numpy as np

# CSV-Datei laden
df = pd.read_csv('finalResult.csv', sep=';')

# Daten vorbereiten
years = ['year1', 'year2', 'year3', 'year4', 'year5', 'year6', 'year7', 'year8']

# Alle Zeilen mit geradem Index (jeder zweite Index) filtern
even_indices = df[df['Index'] % 2 == 0].copy()

# Greedy vs. Optimierer Kategorien basierend auf Index-Bereichen
median_index = even_indices['Index'].median()
greedy_data = even_indices[even_indices['Index'] <= median_index]
optimizer_data = even_indices[even_indices['Index'] > median_index]

# Annahme: Battery Count und PVC sind in verschiedenen Zeilen
# Battery Count = erste Hälfte der Daten, PVC = zweite Hälfte
battery_count_greedy = greedy_data.iloc[:len(greedy_data)//2]
pvc_greedy = greedy_data.iloc[len(greedy_data)//2:]

battery_count_optimizer = optimizer_data.iloc[:len(optimizer_data)//2]
pvc_optimizer = optimizer_data.iloc[len(optimizer_data)//2:]

# Subplot erstellen (1 Zeile, 2 Spalten)
fig = make_subplots(
    rows=1, cols=2,
    subplot_titles=('Greedy', 'Optimierer'),
    horizontal_spacing=0.15
)

# Farben für bessere Unterscheidung
colors_years = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD', '#98FB98', '#F0E68C']

# Greedy Grafik (links)
for i, year in enumerate(years):
    fig.add_trace(
        go.Scatter(
            x=pvc_greedy[year],  # PVC auf X-Achse
            y=battery_count_greedy[year],  # Battery Count auf Y-Achse
            mode='markers',
            name=f'Greedy {year}',
            marker=dict(
                color=colors_years[i],
                size=8,
                symbol='circle'
            ),
            showlegend=True
        ),
        row=1, col=1
    )

# Optimierer Grafik (rechts)
for i, year in enumerate(years):
    fig.add_trace(
        go.Scatter(
            x=pvc_optimizer[year],  # PVC auf X-Achse
            y=battery_count_optimizer[year],  # Battery Count auf Y-Achse
            mode='markers',
            name=f'Optimierer {year}',
            marker=dict(
                color=colors_years[i],
                size=8,
                symbol='diamond'
            ),
            showlegend=True
        ),
        row=1, col=2
    )

# Layout anpassen
fig.update_layout(
    title='Battery Count vs PVC: Greedy vs. Optimierer (Gerade Indizes)',
    width=1400,
    height=600,
    showlegend=True,
    legend=dict(
        orientation="v",
        yanchor="top",
        y=1,
        xanchor="left",
        x=1.02
    )
)

# Achsen formatieren
fig.update_xaxes(title_text="PVC", row=1, col=1)
fig.update_xaxes(title_text="PVC", row=1, col=2)
fig.update_yaxes(title_text="Battery Count", row=1, col=1)
fig.update_yaxes(title_text="Battery Count", row=1, col=2)

# Grid hinzufügen
fig.update_xaxes(showgrid=True, gridwidth=1, gridcolor='lightgray')
fig.update_yaxes(showgrid=True, gridwidth=1, gridcolor='lightgray')

# Anzeigen
fig.show()

# Zusätzliche Statistiken
print("=== Vergleichsstatistiken (Gerade Indizes) ===")
print(f"Battery Count Greedy - Anzahl Datenpunkte: {len(battery_count_greedy)}")
print(f"Battery Count Optimierer - Anzahl Datenpunkte: {len(battery_count_optimizer)}")
print(f"PVC Greedy - Anzahl Datenpunkte: {len(pvc_greedy)}")
print(f"PVC Optimierer - Anzahl Datenpunkte: {len(pvc_optimizer)}")

for year in years:
    print(f"\n{year}:")
    print(f"  Battery Count - Greedy Durchschnitt: {battery_count_greedy[year].mean():.2f}")
    print(f"  Battery Count - Optimierer Durchschnitt: {battery_count_optimizer[year].mean():.2f}")
    print(f"  PVC - Greedy Durchschnitt: {pvc_greedy[year].mean():.2f}")
    print(f"  PVC - Optimierer Durchschnitt: {pvc_optimizer[year].mean():.2f}")