import numpy as np
import csv
import matplotlib.pyplot as plt
from scipy.stats import t
import itertools
import pandas as pd
import seaborn as sns
from os import path

def df_to_csv(filepath,configuration,row,header):
    if path.isfile(filepath):
        with open(filepath, 'a') as fd:
            fd.write(configuration+","+row)
    else:
        with open(filepath, 'w') as fd:
            fd.write(header)
            fd.write(configuration+","+row)