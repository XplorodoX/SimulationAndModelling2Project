import os
import linear_interpolation
import plot

output_path = "interpolated_PV.csv"

if not os.path.exists(output_path):
    linear_interpolation.interpolate_and_format_csv(
        input_path="raw_PV_Data.csv",
        output_path=output_path,
        intervall_min=15
    )

plot.plot(output_path)
