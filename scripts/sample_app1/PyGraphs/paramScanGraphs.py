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

def paramScanGraph():
    folderPath = getConfiguration("folderPath")

    filePath = ''.join([folderPath, '\ite1'])
    all_files = [f for f in listdir(filePath) if isfile(join(filePath, f))]
    unique_configs = Filter(all_files,'MAN_QUEUE')

    lambdas = []
    failed_lambdas = []
    for file in unique_configs:
        # r1 = re.findall('\d+', file )
        lambdas.append(re.findall( r'SIMRESULT_([-+]?\d*\.\d+|\d+)_.*', file))
    # extra_objects_read = get_objects_read(lambdas, failed_lambdas, filtered_files, folderPath)
    for lam in lambdas:
        if lam not in failed_lambdas:
            fileName = Filter(all_files, ''.join(['SIMRESULT_', lam[0][0], '_', lam[0][1]]))
            gridFileName = Filter(fileName,'GRID_LOCATION')
            # accessFilePath = Filter(fileName,'ACCESS')
            # plot_locations(''.join([folderPath, '\ite1\\',gridFileName[0]]),folderPath)

paramScanGraph()