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
from os import listdir
from os.path import isfile, join

def Filter(list, subs):
    filter_data = [i for i in list if subs in i]
    return filter_data

def get_extra_object_reads(lambdas,files,folderPath):
    extra_objects_read = pd.DataFrame()
    for lam in lambdas:
        lambda0 = lam[0][0]
        lambda1 = lam[0][1]
        fileName = Filter(files, ''.join(['SIMRESULT_',lambda0,'_',lambda1]))
        filePath = ''.join([folderPath, '\ite1\\',fileName[0]])
        data = pd.read_csv(filePath, delimiter=';')
        extra_objects_read.at[lambda0,lambda1] = data["Objects Read"].sum() - data["Objects Read"].count()

    extra_objects_read.reset_index(inplace=True)
    extra_objects_read_index=extra_objects_read["index"].astype(float)
    extra_objects_read_index=round(1/extra_objects_read_index,2)
    extra_objects_read=extra_objects_read.iloc[:, 1:]

    extra_objects_read = pd.DataFrame(np.vstack([extra_objects_read.columns, extra_objects_read]))
    extra_objects_read = extra_objects_read.astype(float)
    extra_objects_read.sort_values(by=0, ascending=False, axis=1,inplace=True)
    new_header = extra_objects_read.iloc[0]
    extra_objects_read = extra_objects_read[1:]
    new_header = round(1/new_header,2)
    extra_objects_read.columns = new_header
    extra_objects_read.reset_index(inplace=True)

    extra_objects_read["index"] = extra_objects_read_index
    extra_objects_read.sort_values(by=["index"], ascending=False,inplace=True)
    extra_objects_read.set_index("index",inplace=True)
    # extra_objects_read = extra_objects_read.rename(columns={'index': 'new column name'})
    # df = extra_objects_read.rename_axis('MyIdx').sort_values(by=['MyIdx'])

    return extra_objects_read




def NsfBsfGraph():
    folderPath = getConfiguration("folderPath")
    numOfSimulations = getConfiguration("numOfSimulations")
    startOfMobileDeviceLoop = getConfiguration("startOfMobileDeviceLoop")
    stepOfMobileDeviceLoop = getConfiguration("stepOfMobileDeviceLoop")
    endOfMobileDeviceLoop = getConfiguration("endOfMobileDeviceLoop")
    xTickLabelCoefficient = getConfiguration("xTickLabelCoefficient")
    scenarioType = getConfiguration("scenarioType");
    legends = getConfiguration("legends");
    orchestratorPolicies = getConfiguration("orchestratorPolicy");
    objectPlacements = getConfiguration("objectPlacement");
    numOfMobileDevices = int((endOfMobileDeviceLoop - startOfMobileDeviceLoop) / stepOfMobileDeviceLoop + 1)

    filePath = ''.join([folderPath, '\ite1'])
    all_files = [f for f in listdir(filePath) if isfile(join(filePath, f))]
    filtered_files = Filter(all_files,'READOBJECTS')

    lambdas = []
    for file in filtered_files:
        # r1 = re.findall('\d+', file )
        lambdas.append(re.findall( r'SIMRESULT_(\d+.\d+)_(\d+.\d+)_.*', file))
    extra_objects_read = get_extra_object_reads(lambdas,filtered_files,folderPath)
    g=sns.heatmap(extra_objects_read,cbar_kws={'label': 'Parities Read'},cmap=sns.cm.rocket_r)

    g.set(xlabel='λ_a', ylabel='λ_b')
    g.set_title("Service Cost")
    g.savefig(folderPath + '\\fig\\Service Cost' + '.png', bbox_inches='tight')
    plt.show()


NsfBsfGraph()

