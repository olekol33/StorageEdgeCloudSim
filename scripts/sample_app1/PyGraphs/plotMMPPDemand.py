from getConfiguration import getConfiguration
import numpy as np
import csv
import matplotlib.pyplot as plt
from scipy.stats import t
import itertools
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.ticker import MaxNLocator
from os import path
import seaborn as sns
import re
from os import listdir,makedirs
from os.path import isfile, join, exists


def plotMMPPDemand():
    folderPath = getConfiguration("folderPath")
    filePath = ''.join([folderPath, '\ite1\MMPP\\'])
    figPath = ''.join([folderPath, '\ite1\MMPP\\fig\\'])
    if not exists(figPath):
        makedirs(figPath)
    all_files = [f for f in listdir(filePath) if isfile(join(filePath, f))]
    burst_files = []
    burst_objects = pd.DataFrame()
    for file in all_files:
        lam = re.findall(r'SIMRESULT_.*(D\d+)_.*', file)
        fig, ax = plt.subplots(1, 1,figsize=(20,10))
        data = pd.read_csv(filePath+file,delimiter=';')
        if max(data['Requests'] > 100):
            burst_objects[lam[0]] = data["Requests"]
        data.plot(x='Interval',y='Requests',ax=ax)
        ax.title.set_text(lam[0])
        ax.legend().set_visible(False)
        fig.savefig(figPath + lam[0] + '.png', bbox_inches='tight',
                    format='png')
        plt.close()
    fig, ax = plt.subplots(1, 1,figsize=(20,10))
    # burst_objects.plot(x=burst_objects.index.values, y=burst_objects.columns, kind="bar")
    burst_objects.plot(ax=ax)
    ax.title.set_text("Bursty Objects")
    fig.savefig(figPath + 'Bursty Objects.png', bbox_inches='tight',
                format='png')
    fig.show()



plotMMPPDemand()