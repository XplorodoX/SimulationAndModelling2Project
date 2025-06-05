diff --git a/README.md b/README.md
index ed9de64e16a5056926074b71464dea07a7e2b438..1147c41dabd2d77edfffa789fa14c56057dd134a 100644
--- a/README.md
+++ b/README.md
@@ -34,81 +34,83 @@ Decentralized energy systems are becoming increasingly important for reducing co
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
-├── model/                       # AnyLogic project files
-│   ├── PV_Battery_Optimization.als
-│   └── scripts/                # JavaScript & Python scripts
-│       ├── run_simulation.js
-│       └── analyze_results.py
-├── docs/                        # Background documentation
-│   ├── Simulation_and_Modelling2_Project.pdf  # LaTeX report (Overleaf)
-│   └── UML_Diagram.png          # UML diagram (Lucidchart export)
-├── data/                        # Input data (CSV, JSON)
-├── project-plan/                # GitHub project board link
-├── README.md                    # Project overview (this file)
-└── LICENSE
+├── Database/                 # Java utilities for importing data
+│   ├── AnyLogicDBUtil.java
+│   ├── CsvImporter.java
+│   └── DatabaseTest.java
+├── PV/                       # Simple PV model classes
+│   ├── PV.java
+│   └── additionalPV.txt
+├── IntermdianPresentation/   # Slides for the intermediate presentation
+│   └── Timeline.pptx
+└── README.md                 # Project overview (this file)
 ```
 
 ---
 
 ## Installation & Usage
 
 1. Install **AnyLogic Community Edition** or **University License**.
 
 2. Clone the repository:
 
    ```bash
    git clone https://github.com/<your-github-username>/SimulationAndModelling2Project.git
    cd SimulationAndModelling2Project
    ```
-3. Open the simulation in AnyLogic:
+3. Open your AnyLogic model (e.g., `PV_Battery_Optimization.als`),
+   adjust parameters such as budget, module prices and tariff data,
+   then launch the simulation.
 
-   * Open `model/PV_Battery_Optimization.als`
-   * Adjust parameters (budget, module prices, tariff data)
-   * Select run configuration and launch simulation
+4. To build the helper tools in `Database/`, run:
+
+   ```bash
+   javac -cp "Database/jar/*" Database/*.java PV/*.java
+   ```
 
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
