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
from matplotlib import ticker
from collections import Counter


#Rows
TotalTasks = 0
EdgeTasks = 1
CloudTasks = 2
DeviceTasks = 3
NetworkData = 4

#Columns
completedTask = 0
failedTask = 1
uncompletedTask = 2
failedTaskDuetoBw = 3
serviceTime = 4
processingTime = 5
networkDelay = 6
cost = 8
vmLoadOnClould = 8
failedTaskDueToVmCapacity = 9
failedTaskDuetoMobility = 10
failedTaskDuetoPolicy = 11
failedTaskDuetoQueue = 12
failedTaskDuetoInaccessibility = 13

lanDelay = 0
manDelay = 1
wanDelay = 2
failedTaskDuetoLanBw = 4
failedTaskDuetoManBw = 5
failedTaskDuetoWanBw = 6

def Filter(list, subs):
    filter_data = [i for i in list if subs in i]
    return filter_data

def extractConfiguration(filename):
    orchestrator_policies= ["IF_CONGESTED_READ_PARITY","NEAREST_HOST","CLOUD_OR_NEAREST_IF_CONGESTED"]
    object_placements=["CODING_PLACE","REPLICATION_PLACE","DATA_PARITY_PLACE"]
    simulation_scenarios=["SINGLE_TIER","TWO_TIER","TWO_TIER_WITH_EO"]
    fail_policies=["WITHFAIL","NOFAIL"]
    dist_policies=["UNIFORM","ZIPF","MMPP","MULTIZIPF"]
    oh=""
    lam=""
    if "_OH" in filename:
        oh = re.findall(r'.*_OH([-+]?\d*\.\d+|\d+)_.*', filename)
    else:
        lam = re.findall(r'SIMRESULT_([-+]?\d*\.\d+|\d+)_.*', filename)
    for item in orchestrator_policies:
        if item in filename:
            orchestrator_policy=item
    for item in object_placements:
        if item in filename:
            object_placement=item
    for item in simulation_scenarios:
        if item in filename:
            simulation_scenario=item
    for item in fail_policies:
        if item in filename:
            fail=item
    for item in dist_policies:
        if item in filename:
            dist=item
    if oh != "":
        return {'value': float(oh[0]), 'orchestrator_policy': orchestrator_policy,
                'object_placement': object_placement,
                'simulation_scenario': simulation_scenario, 'fail': fail, 'distribution': dist}
    else:
        return {'value':float(lam[0]),'orchestrator_policy':orchestrator_policy, 'object_placement':object_placement,
                'simulation_scenario':simulation_scenario, 'fail':fail, 'distribution':dist}


def paramScanGraph():
    dataObjects = "Overhead fail=2% lambda=0.38 fully balanced"
    folderPath = getConfiguration("folderPath")
    overhead_mode=False

    filePath = ''.join([folderPath, '\ite1'])
    all_files = [f for f in listdir(filePath) if isfile(join(filePath, f))]
    completed_files = Filter(all_files,'COMPLETED')
    oh = Filter(all_files,'_OH')
    if len(oh)>0:
        overhead_mode=True
    runs = []
    for file in completed_files:
        runs.append(extractConfiguration(file))

    runsPD = pd.DataFrame(columns=["value","orchestrator_policy","object_placement","simulation_scenario",
                                   "fail","distribution"])

    for run in runs:
        runsPD = runsPD.append(run, ignore_index=True)

    fig,ax = plt.subplots(2,1,figsize=(10,8))
    runsPD[runsPD["fail"]=="NOFAIL"].plot.barh(color = 'red',ax=ax[0])
    runsPD[runsPD["fail"]=="WITHFAIL"].plot.barh(color = 'blue',ax=ax[1])

    ax[0].set_yticklabels(runsPD[runsPD["fail"]=="NOFAIL"].orchestrator_policy + "\n" + runsPD[runsPD["fail"]=="NOFAIL"].object_placement
                          + "\n" + runsPD[runsPD["fail"]=="NOFAIL"].fail + "\n" + runsPD[runsPD["fail"]=="NOFAIL"].distribution)
    ax[0].title.set_text("No Fail")

    ax[1].set_yticklabels(runsPD[runsPD["fail"]=="WITHFAIL"].orchestrator_policy + "\n" + runsPD[runsPD["fail"]=="WITHFAIL"].object_placement
                          + "\n" + runsPD[runsPD["fail"]=="WITHFAIL"].fail + "\n" + runsPD[runsPD["fail"]=="WITHFAIL"].distribution)
    ax[1].title.set_text("With Fail")
    # sns.barplot(x="lambda", y=runsPD.index, data=runsPD, hue='fail',ax=ax)
    for a in ax:
        start, end = a.get_xlim()
        a.grid(axis='x')
        if (overhead_mode):
            a.set_xlim([2, 5.5])
            start, end = a.get_xlim()
            a.xaxis.set_ticks(np.arange(start, end, 0.5))
            a.set_xlabel('Overhead')
        else:
            a.set_xlim([0.1, 0.35])
            start, end = a.get_xlim()
            a.xaxis.set_ticks(np.arange(start, end, 0.05))
            a.set_xlabel('lambda[mu]')
        a.legend().set_visible(False)
    plt.show()
    fig.suptitle("" + dataObjects+ "",y=1.02)
    fig.savefig(folderPath + '\\fig\\' + dataObjects+ '.png', bbox_inches='tight', format ='png')
paramScanGraph()