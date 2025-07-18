import pandas as pd
import glob
import os

# Pfad zum Skript selbst ermitteln
script_dir = os.path.dirname(os.path.abspath(__file__))

# Den Pfad zum Unterordner 'CSV' erstellen
pfad_zu_csvs = os.path.join(script_dir, 'CSV')

print(f"Suche nach CSV-Dateien in: {pfad_zu_csvs}")

# Liste aller CSV-Dateien im Ordner erstellen
alle_dateien = glob.glob(os.path.join(pfad_zu_csvs, "*.csv"))

# Prüfen, ob Dateien gefunden wurden
if not alle_dateien:
    print("❌ Fehler: Keine CSV-Dateien im Ordner 'CSV' gefunden.")
    print("Bitte stelle sicher, dass der Ordner existiert und .csv-Dateien enthält.")
else:
    dataframes_liste = []
    for datei in alle_dateien:
        print(f"Lese Datei: {os.path.basename(datei)}")
        try:
            # Lese CSV und setze die erste Spalte als Index
            df = pd.read_csv(datei, index_col=0)
            dataframes_liste.append(df)
        except Exception as e:
            print(f"Konnte die Datei {datei} nicht laden: {e}")

    # Nur fortfahren, wenn erfolgreich DataFrames geladen wurden
    if dataframes_liste:
        # Alle DataFrames zusammenfügen (nebeneinander basierend auf dem Index)
        zusammengefuegter_df = pd.concat(dataframes_liste, axis=1)

        # --- NEU 1: Doppelte Indexe entfernen ---
        # Stellt sicher, dass jede Indexzeile nur einmal vorkommt.
        # 'keep="first"' behält das erste Vorkommen.
        zusammengefuegter_df = zusammengefuegter_df[~zusammengefuegter_df.index.duplicated(keep='first')]

        # --- NEU 2: Index auf Vollständigkeit prüfen ---
        print("\nPrüfe Index auf Vollständigkeit...")
        try:
            # Versuche, den Index in Zahlen umzuwandeln.
            zusammengefuegter_df.index = pd.to_numeric(zusammengefuegter_df.index)

            # Sortiere den DataFrame nach dem numerischen Index
            zusammengefuegter_df.sort_index(inplace=True)

            # Erstelle den erwarteten, vollständigen Indexbereich
            erwarteter_index = set(range(zusammengefuegter_df.index.min(), zusammengefuegter_df.index.max() + 1))

            # Hole den tatsächlichen Index
            tatsaechlicher_index = set(zusammengefuegter_df.index)

            # Vergleiche beide und finde die fehlenden Werte
            fehlende_werte = erwarteter_index - tatsaechlicher_index

            if not fehlende_werte:
                print("✅ Der Index ist fortlaufend und vollständig.")
            else:
                print(f"❌ Warnung: Der Index ist nicht vollständig. Es fehlen {len(fehlende_werte)} Werte.")
                # Zeige nur die ersten 10 fehlenden Werte an, um die Ausgabe nicht zu überfluten
                print(f"   Fehlende Indexe (Beispiele): {sorted(list(fehlende_werte))[:10]}")

        except ValueError:
            print("❌ Fehler: Der Index konnte nicht in Zahlen umgewandelt werden. Prüfung abgebrochen.")

        # Speicherort für die Ausgabedatei (im selben Ordner wie das Skript)
        output_path = os.path.join(script_dir, 'finalResult.csv')
        zusammengefuegter_df.to_csv(output_path)

        print(f"\n✅ Erfolgreich {len(dataframes_liste)} CSV-Dateien zusammengefügt in '{output_path}'!")