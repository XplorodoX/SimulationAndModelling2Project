import pandas as pd
import plotly.graph_objects as go
from plotly.subplots import make_subplots
import numpy as np
import plotly.io as pio
pio.renderers.default = "browser"


test = pd.read_csv('finalResultSubtracted.csv', sep=";")
# Funktion zur Dekodierung des Index
def decode_index(index):
    """
    Dekodiert den Index in strategy, pvCount, batteryCount
    """
    battery_count = index // 32
    remaining = index % 32
    pv_count = remaining // 2
    strategy = remaining % 2
    return strategy, pv_count, battery_count

def costPV(kwp):
    if kwp == 0:
        return 0
    else:
        return 900*kwp + 4500
    
def CalRow(bCount, pvCount, bCost):
    d1 = 0.4
    d2 = 0.2

    c1 = bCost * bCount
    c2 = costPV(pvCount*0.88)

    return (d1*c1 + d2*c2) / (c1 + c2 + 1e-9), c1, c2, d1, d2

solutions = np.zeros(len(test['Index']))
for i in test['Index'].astype(int):
    strategy, pv_count, battery_count = decode_index(int(i))
    solution, c1, c2, d1, d2 = CalRow(battery_count, pv_count, 2500)
    solutions[int(i)] = solution
    test.loc[int(i), 'year1':'year8'] = test.loc[int(i), 'year1':'year8']-(c1*d1+c2*d2)
test.to_csv('zwischen3.csv', sep=';', index=False)

# CSV-Datei laden
df = pd.read_csv('zwischen3.csv', sep=";")

# Daten dekodieren
decoded_data = []
for idx, row in df.iterrows():
    if pd.notna(row['Index']):
        strategy, pv_count, battery_count = decode_index(int(row['Index']))
        decoded_data.append({
            'Index': row['Index'],
            'strategy': strategy,
            'pvCount': pv_count,
            'batteryCount': battery_count,
            'year1': row['year1'],
            'year2': row['year2'],
            'year3': row['year3'],
            'year4': row['year4'],
            'year5': row['year5'],
            'year6': row['year6'],
            'year7': row['year7'],
            'year8': row['year8']
        })

# DataFrame erstellen
decoded_df = pd.DataFrame(decoded_data)

# Daten nach Strategy trennen
greedy_data = decoded_df[decoded_df['strategy'] == 0].copy()
optimizer_data = decoded_df[decoded_df['strategy'] == 1].copy()

# Heatmap-Daten für Year 8 vorbereiten
def create_heatmap_data(data, year='year8'):
    """Erstellt eine Matrix für die Heatmap"""
    # Pivot-Tabelle erstellen
    pivot_data = data.pivot(index='batteryCount', columns='pvCount', values=year)
    return pivot_data

# Heatmap-Daten erstellen
greedy_heatmap = create_heatmap_data(greedy_data, 'year8')
optimizer_heatmap = create_heatmap_data(optimizer_data, 'year8')

# Subplot für Heatmaps erstellen
fig = make_subplots(
    rows=1, cols=2,
    subplot_titles=('Greedy Strategy', 'Optimizer Strategy'),
    horizontal_spacing=0.1
)

# Greedy Heatmap
fig.add_trace(
    go.Heatmap(
        z=greedy_heatmap.values,
        x=greedy_heatmap.columns,
        y=greedy_heatmap.index,
        coloraxis = "coloraxis",
        #colorscale='RdYlBu_r',
        name='Greedy',
        showscale=True,
        #colorbar=dict(x=0.45, len=0.8)
    ),
    row=1, col=1
)

# Optimizer Heatmap
fig.add_trace(
    go.Heatmap(
        z=optimizer_heatmap.values,
        x=optimizer_heatmap.columns,
        y=optimizer_heatmap.index,
        coloraxis = "coloraxis",
        #colorscale='RdYlBu_r',
        name='Optimizer',
        showscale=True,
        #colorbar=dict(x=1.02, len=0.8)
    ),
    row=1, col=2
)

fig.update_layout(coloraxis = {'colorscale':'RdYlBu_r'})

# Layout anpassen
fig.update_layout(
    title='Value gained after 8 years',
    width=1200,
    height=500,
    title_x=0.5
)

# Achsen formatieren
fig.update_xaxes(title_text="PV Count", row=1, col=1)
fig.update_xaxes(title_text="PV Count", row=1, col=2)
fig.update_yaxes(title_text="Battery Count", row=1, col=1)
fig.update_yaxes(title_text="Battery Count", row=1, col=2)

# Anzeigen
fig.show()

# Verbesserungsmatrix erstellen
improvement_matrix = ((optimizer_heatmap - greedy_heatmap) / (abs(greedy_heatmap)+1e-9)) * 100
improvement_matrix.iloc[:, 0] = np.inf

# Verbesserungs-Heatmap erstellen
fig_improvement = go.Figure(data=go.Heatmap(
    z=improvement_matrix.values,
    x=improvement_matrix.columns,
    y=improvement_matrix.index,
    colorscale='RdYlGn',
    colorbar=dict(title="Verbesserung (%)")
))

fig_improvement.update_layout(
    title='Verbesserung durch Optimizer vs Greedy (Year 8)<br>X: PV Count, Y: Battery Count',
    xaxis_title="PV Count",
    yaxis_title="Battery Count",
    width=800,
    height=500
)

fig_improvement.show()

# Detaillierte Analyse
print("=== Detaillierte Analyse Year 8 ===")
print(f"Beste Greedy-Konfiguration:")
best_greedy_idx = greedy_data['year8'].idxmax()
best_greedy = greedy_data.loc[best_greedy_idx]
print(f"  PV: {best_greedy['pvCount']}, Battery: {best_greedy['batteryCount']}")
print(f"  Ergebnis: {best_greedy['year8']:.2f}")

print(f"\nBeste Optimizer-Konfiguration:")
best_optimizer_idx = optimizer_data['year8'].idxmax()
best_optimizer = optimizer_data.loc[best_optimizer_idx]
print(f"  PV: {best_optimizer['pvCount']}, Battery: {best_optimizer['batteryCount']}")
print(f"  Ergebnis: {best_optimizer['year8']:.2f}")

print(f"\nGrößte Verbesserung:")
max_improvement_idx = improvement_matrix.stack().idxmax()
battery_max, pv_max = max_improvement_idx
print(f"  PV: {pv_max}, Battery: {battery_max}")
print(f"  Verbesserung: {improvement_matrix.loc[battery_max, pv_max]:.2f}%")

# Durchschnittliche Verbesserung pro Batterie-Level
print(f"\nDurchschnittliche Verbesserung pro Battery Count:")
for battery_count in range(6):
    avg_improvement = improvement_matrix.loc[battery_count].mean()
    print(f"  Battery {battery_count}: {avg_improvement:.2f}%")

# Durchschnittliche Verbesserung pro PV-Level
print(f"\nDurchschnittliche Verbesserung pro PV Count (erste 5):")
for pv_count in range(5):
    avg_improvement = improvement_matrix[pv_count].mean()
    print(f"  PV {pv_count}: {avg_improvement:.2f}%")
