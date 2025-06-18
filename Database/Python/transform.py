import pandas as pd

def transformiere_zeitstempel(df: pd.DataFrame, spaltenname: str = 'utc_timestamp') -> pd.DataFrame:
    """
    Wandelt das Zeitformat der angegebenen Spalte um und addiert 15 Minuten.
    """
    df[spaltenname] = pd.to_datetime(df[spaltenname], utc=True)
    df[spaltenname] = df[spaltenname] + pd.Timedelta(minutes=15)
    df[spaltenname] = df[spaltenname].dt.strftime('%Y-%m-%d %H:%M:%S')
    return df

def entferne_spalte(df: pd.DataFrame, spaltenname: str) -> pd.DataFrame:
    """
    Entfernt die angegebene Spalte aus dem DataFrame.
    """
    return df.drop(columns=[spaltenname])

if __name__ == '__main__':
    input_pfad = 'oldi/household_data_15min_singleindex.csv'
    output_pfad = 'household_data_15min_singleindex.csv'
    zeitspalte = 'utc_timestamp'
    zu_entfernende_spalte = 'cet_cest_timestamp'

    df = pd.read_csv(input_pfad)
    df = transformiere_zeitstempel(df, zeitspalte)
    if zu_entfernende_spalte in df.columns:
        df = entferne_spalte(df, zu_entfernende_spalte)
    df.to_csv(output_pfad, index=False)