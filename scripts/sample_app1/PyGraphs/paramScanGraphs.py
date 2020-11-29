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
    dist_policies=["UNIFORM","ZIPF"]
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

    return {'lambda':float(lam[0]),'orchestrator_policy':orchestrator_policy, 'object_placement':object_placement,
            'simulation_scenario':simulation_scenario, 'fail':fail, 'distribution':dist}


def paramScanGraph():
    dataObjects = "70"
    folderPath = getConfiguration("folderPath")

    filePath = ''.join([folderPath, '\ite1'])
    all_files = [f for f in listdir(filePath) if isfile(join(filePath, f))]
    completed_files = Filter(all_files,'COMPLETED')
    runs = []
    for file in completed_files:
        runs.append(extractConfiguration(file))

    runsPD = pd.DataFrame(columns=["lambda","orchestrator_policy","object_placement","simulation_scenario",
                                   "fail","distribution"])

    for run in runs:
        runsPD = runsPD.append(run, ignore_index=True)

    fig,ax = plt.subplots(1,1,figsize=(10,12))
    runsPD.plot.barh(ax=ax)
    ax.set_yticklabels(runsPD.orchestrator_policy+ "\n" + runsPD.object_placement+ "\n" +runsPD.fail + "\n" +
                       runsPD.distribution)
    ax.set_xlim([0.3,1.2])
    start, end = ax.get_xlim()
    ax.xaxis.set_ticks(np.arange(start, end, 0.05))
    ax.grid(axis='x')
    ax.set_xlabel('lambda[mu]')
    ax.legend().set_visible(False)
    plt.show()
    ax.title.set_text("Corner Cases " + dataObjects+ " data objects")
    fig.savefig(folderPath + '\\fig\\Corner Cases ' + dataObjects+ ' data objects.png', bbox_inches='tight', format ='png')
paramScanGraph()