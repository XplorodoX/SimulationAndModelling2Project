# Optimization of Photovoltaic and Battery Storage Systems

**Kurs:** Simulation and Modelling II
**Datum:** Mai 2025
**Autoren:** Robert Leichtl, Florian Merlau, Jakob Haberstock, Christian Besold

---

## Projektübersicht

In diesem Projekt entwickeln wir ein simulationsbasiertes Optimierungsmodell für Photovoltaik- (PV) und Batteriespeichersysteme. Ziel ist es, unter Berücksichtigung von Budget-, Kapazitäts- und Lastprofil-Restriktionen die Kombination aus PV-Modulanzahl, Batteriespeicherkapazität und Handelsstrategie so zu wählen, dass der Nettogewinn (Einsparungen abzüglich Investitions- und Betriebskosten) maximiert wird.

---

## Motivation

Dezentrale Energiesysteme gewinnen zunehmend an Bedeutung, um Kosten zu senken und die Energie­autarkie zu erhöhen. Eine optimal dimensionierte Kombination aus PV-Anlage und Batteriespeicher erlaubt:

* Minimierung von Netzbezugskosten
* Maximierung des Eigenverbrauchs
* Flexiblen Handel von Überschuss­energie

---

## Kernelemente der Simulation

1. **Saisonale Einstrahlungsprofile**
2. **Stromverbrauch von Haushalt, E-Mobilität und Wärmepumpe**
3. **Leistungskennlinie und COP (Coefficient Of Performance) der Wärmepumpe**
4. **Tarifmodell mit variablen Einspeise- und Bezugspreisen**
5. **Entscheidungsvariablen**

   * Anzahl und Leistung der PV-Module
   * Batterie­kapazität und C-Rate
   * Handelsstrategie (Zeitpunkte für Einspeisung bzw. Netzbezug)

---

## Implementierungsdetails

* **Simulationsframework:** AnyLogic (mit JavaScript- und Python-Scripting)
* **Optimierung:** parametrische Suche / heuristische Verfahren
* **Dateninput:**

  * Zeitreihen für Globalstrahlung (1-h Auflösung)
  * Verbrauchsprofile (24-h-Lastkurven)
  * Preis- und Tarifdaten
* **Visualisierung & Auswertung:**

  * AnyLogic-Charts
  * Externe Python-Skripte für Batch-Runs und Ergebnis­aggregation

---

## Projektstruktur

```
.
├── model/                       # AnyLogic-Projektdateien
│   ├── PV_Battery_Optimization.als
│   └── scripts/                # JavaScript- & Python-Skripte
│       ├── run_simulation.js
│       └── analyze_results.py
├── docs/                        # Hintergrunddokumentation
│   ├── Simulation_and_Modelling2_Project.pdf  # LaTeX-Report (Overleaf)
│   └── UML_Diagramm.png         # UML (Lucidchart-Export)
├── data/                        # Input-Daten (CSV, JSON)
├── project-plan/                # GitHub-Projektplan-Link
├── README.md                    # Projekt-Übersicht
└── LICENSE
```

---

## Installation & Start

1. **AnyLogic Community Edition** oder **University License** installieren.

2. Repository klonen:

   ```bash
   git clone https://github.com/<ihr-github-user>/SimulationAndModelling2Project.git
   cd SimulationAndModelling2Project
   ```

3. **Python-Umgebung** einrichten (optional für Batch-Skripte):

   ```bash
   python3 -m venv venv
   source venv/bin/activate
   pip install -r requirements.txt
   ```

4. **Simulation in AnyLogic** öffnen:

   * Datei `model/PV_Battery_Optimization.als` öffnen
   * Parameter (Budget, Modulpreise, Tarifdaten) anpassen
   * Run Configuration auswählen und Simulation starten

5. **Batch-Runs** per Kommandozeile:

   ```bash
   # Einzelnen Szenario-Run
   python model/scripts/analyze_results.py --input data/results_run1.csv
   ```

---

## Weiterführende Dokumentation

* **LaTeX-Report (Overleaf):**
  [https://www.overleaf.com/read/pjqyfxrzmhpv#ac28a1](https://www.overleaf.com/read/pjqyfxrzmhpv#ac28a1)

* **UML-Diagramm (Lucidchart):**
  [https://lucid.app/lucidchart/37222884-5f2d-4537-a731-513d22f26cf5/edit?page=HWEp-vi-RSFO\&invitationId=inv\_09532988-f101-4204-8e33-1ce2f0ec578a#](https://lucid.app/lucidchart/37222884-5f2d-4537-a731-513d22f26cf5/edit?page=HWEp-vi-RSFO&invitationId=inv_09532988-f101-4204-8e33-1ce2f0ec578a#)

---

## Projektplan

Der detaillierte Projektplan ist unter folgendem GitHub Project Board erreichbar:
[https://github.com/users/XplorodoX/projects/6/views/1](https://github.com/users/XplorodoX/projects/6/views/1)

---

## Lizenz

Dieses Projekt steht unter der MIT-Lizenz. Details siehe [LICENSE](LICENSE).

---
