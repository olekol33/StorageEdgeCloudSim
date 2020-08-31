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
                host_frame = {}
                access_frame = {}
                fig, ax = plt.subplots(1, 1)
                fig2, ax2 = plt.subplots(1, 1)
                for o, orchestratorPolicy in enumerate(orchestratorPolicies):
                    mobileDeviceNumber = startOfMobileDeviceLoop + stepOfMobileDeviceLoop * j
                    filePath = ''.join([folderPath, '\ite', str(s + 1), '\SIMRESULT_', str(scenarioType[i]), '_',
                                        orchestratorPolicy, '_', str(mobileDeviceNumber), 'DEVICES_OBJECTS.log'])
                    data = pd.read_csv(filePath, delimiter=';')
                    host_series = data['HostID'].value_counts().sort_index()
                    access_series = data['AccessID'].value_counts().sort_index()
                    host_frame[orchestratorPolicy] = host_series
                    access_frame[orchestratorPolicy] = access_series
                    # plt.show()
                placedObjects = pd.DataFrame(host_frame)
                accessedHosts = pd.DataFrame(access_frame)

                placedObjects.plot(y=orchestratorPolicies, kind="bar", use_index=True, ax=ax)
                ax.set_xlabel("Host Number")
                ax.set_ylabel("Read Objects")
                fig.suptitle("Placed_Objects_" + str(mobileDeviceNumber))
                fig.savefig(folderPath + '\\fig\\Placed_Objects_' + str(mobileDeviceNumber) + '.png', bbox_inches='tight')
                plt.close(fig)

                accessedHosts.plot(y=orchestratorPolicies, kind="bar", use_index=True, ax=ax2)
                ax2.set_xlabel("Host Number")
                ax2.set_ylabel("Read Objects")
                fig2.suptitle("Accessed_Hosts_" + str(mobileDeviceNumber))
                fig2.savefig(folderPath + '\\fig\\Accessed_Hosts_' + str(mobileDeviceNumber) + '.png', bbox_inches='tight')
                plt.close(fig2)

# plotReadObjects()
