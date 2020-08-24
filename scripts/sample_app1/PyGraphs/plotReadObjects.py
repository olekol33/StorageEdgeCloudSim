from getConfiguration import getConfiguration
import numpy as np
import csv
import matplotlib.pyplot as plt
from scipy.stats import t
import itertools
import pandas as pd
import matplotlib.pyplot as plt

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

lanDelay = 0
manDelay = 1
wanDelay = 2
failedTaskDuetoLanBw = 4
failedTaskDuetoManBw = 5
failedTaskDuetoWanBw = 6

def plotReadObjects():
    folderPath = getConfiguration("folderPath")
    numOfSimulations = getConfiguration("numOfSimulations")
    startOfMobileDeviceLoop = getConfiguration("startOfMobileDeviceLoop")
    stepOfMobileDeviceLoop = getConfiguration("stepOfMobileDeviceLoop")
    endOfMobileDeviceLoop = getConfiguration("endOfMobileDeviceLoop")
    xTickLabelCoefficient = getConfiguration("xTickLabelCoefficient")
    scenarioType = getConfiguration("scenarioType");
    legends = getConfiguration("legends");
    orchestratorPolicies = getConfiguration("orchestratorPolicy");
    numOfMobileDevices = int((endOfMobileDeviceLoop - startOfMobileDeviceLoop) / stepOfMobileDeviceLoop + 1)
#    pos = getConfiguration(9);



    # initializing the titles and rows list
    for s in range(numOfSimulations):
        for i in range(len(scenarioType)):
            for j in range(numOfMobileDevices):
                frame = {}
                fig, ax = plt.subplots(1, 1)
                for o, orchestratorPolicy in enumerate(orchestratorPolicies):
                    mobileDeviceNumber = startOfMobileDeviceLoop + stepOfMobileDeviceLoop * j
                    filePath = ''.join([folderPath, '\ite', str(s + 1), '\SIMRESULT_', str(scenarioType[i]), '_',
                                        orchestratorPolicy, '_', str(mobileDeviceNumber), 'DEVICES_OBJECTS.log'])
                    data = pd.read_csv(filePath, delimiter=';')
                    series = data['HostID'].value_counts().sort_index()
                    frame[orchestratorPolicy] = series
                    # plt.show()
                placedObjects = pd.DataFrame(frame)
                placedObjects.plot(y=orchestratorPolicies, kind="bar", use_index=True, ax=ax)
                ax.set_xlabel("Host Number")
                ax.set_ylabel("Read Objects")
                fig.savefig(folderPath + '\\fig\\Placed_Objects_' + str(mobileDeviceNumber) + '.png', bbox_inches='tight')
                plt.close(fig)

plotReadObjects()
