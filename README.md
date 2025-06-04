# Optimization of Photovoltaic and Battery Storage Systems

**Course:** Simulation and Modelling II
**Date:** May 2025
**Authors:** Robert Leichtl, Florian Merlau, Jakob Haberstock, Christian Besold

---

## Project Overview

In this project, we develop a simulation-based optimization model for photovoltaic (PV) and battery storage systems. The objective is to determine the optimal combination of PV module count, battery capacity, and trading strategy to maximize net savings (cost reductions minus investment and operating costs), subject to budget, capacity, and load-profile constraints.

---

## Motivation

Decentralized energy systems are becoming increasingly important for reducing costs and enhancing energy self-sufficiency. An optimally sized PV and battery system enables:

* Minimization of grid purchase costs
* Maximization of self-consumption
* Flexible trading of surplus energy

---

## Core Simulation Elements

1. **Seasonal solar irradiation profiles**
2. **Electricity consumption of household and heat pump**
3. **Performance curve and COP (Coefficient of Performance) of the heat pump**
4. **Tariff model with variable feed-in and purchase prices**
5. **Decision Variables:**

   * Number and capacity of PV modules
   * Battery capacity and C-rate
   * Trading strategy (timing of feed-in vs. grid draw)

---

## Implementation Details

* **Simulation Framework:** AnyLogic (with JavaScript and Python scripting)
* **Optimization Method:** Parametric search / heuristic algorithms
* **Data Inputs:**

  * Time series of global horizontal irradiance (1-hour resolution)
  * Load profiles (24-hour curves)
  * Price and tariff data
* **Visualization & Analysis:**

  * AnyLogic built-in charts
  * External Python scripts for batch runs and result aggregation

---

## Project Structure

```
.
├── model/                       # AnyLogic project files
│   ├── PV_Battery_Optimization.als
│   └── scripts/                # JavaScript & Python scripts
│       ├── run_simulation.js
│       └── analyze_results.py
├── docs/                        # Background documentation
│   ├── Simulation_and_Modelling2_Project.pdf  # LaTeX report (Overleaf)
│   └── UML_Diagram.png          # UML diagram (Lucidchart export)
├── data/                        # Input data (CSV, JSON)
├── project-plan/                # GitHub project board link
├── README.md                    # Project overview (this file)
└── LICENSE
```

---

## Installation & Usage

1. Install **AnyLogic Community Edition** or **University License**.

2. Clone the repository:

   ```bash
   git clone https://github.com/<your-github-username>/SimulationAndModelling2Project.git
   cd SimulationAndModelling2Project
   ```
3. Open the simulation in AnyLogic:

   * Open `model/PV_Battery_Optimization.als`
   * Adjust parameters (budget, module prices, tariff data)
   * Select run configuration and launch simulation

---

## Further Documentation

* **LaTeX Report (Overleaf):**
  [https://www.overleaf.com/read/pjqyfxrzmhpv#ac28a1](https://www.overleaf.com/read/pjqyfxrzmhpv#ac28a1)
* **LaTeX intermediate Presentation (Overleaf):**
  [https://www.overleaf.com/read/hzhfkzznyqty#d170c5](https://www.overleaf.com/read/hzhfkzznyqty#d170c5)

* **UML Diagram (Lucidchart):**
  [https://lucid.app/lucidchart/37222884-5f2d-4537-a731-513d22f26cf5/edit?page=HWEp-vi-RSFO\&invitationId=inv\_09532988-f101-4204-8e33-1ce2f0ec578a#](https://lucid.app/lucidchart/37222884-5f2d-4537-a731-513d22f26cf5/edit?page=HWEp-vi-RSFO&invitationId=inv_09532988-f101-4204-8e33-1ce2f0ec578a#)

* **Template Presentation:**
  [https://gitlab.cs.fau.de/schaefer/i7-beamer-latex-template](https://gitlab.cs.fau.de/schaefer/i7-beamer-latex-template)

---

## Database Example

The `Database` folder now contains a lightweight Java utility that imports CSV
or Excel files directly into AnyLogic's internal database. Table structures are
created automatically from the file headers, so no manual schema definition is
required. Usage instructions can be found in
[`Database/README.md`](Database/README.md).

---

## Project Plan

The detailed project plan is accessible via the GitHub Project board:
[https://github.com/users/XplorodoX/projects/6/views/1](https://github.com/users/XplorodoX/projects/6/views/1)

