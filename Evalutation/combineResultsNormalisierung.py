import pandas as pd
import glob
import os

df = pd.read_csv("finalResult.csv", index_col=0)

df.set_index("Index", inplace=True)

df.sort_values("Index")

row_zweo = df.loc[0]

df_subtracted = df - row_zweo

df_subtracted.to_csv("finalResultSubtracted.csv")