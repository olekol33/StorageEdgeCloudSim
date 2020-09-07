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
    objectPlacements = getConfiguration("objectPlacement");
    numOfMobileDevices = int((endOfMobileDeviceLoop - startOfMobileDeviceLoop) / stepOfMobileDeviceLoop + 1)
#    pos = getConfiguration(9);



    # initializing the titles and rows list
    for s in range(numOfSimulations):
        for i in range(len(scenarioType)):
            for j in range(numOfMobileDevices):
                host_frame = {}
                access_frame = {}
                fig, ax = plt.subplots(len(objectPlacements), 1, figsize=(10, 17))
                fig2, ax2 = plt.subplots(len(objectPlacements), 1, figsize=(10, 17))
                for p, objectPlacement in enumerate(objectPlacements):
                    for o, orchestratorPolicy in enumerate(orchestratorPolicies):
                        mobileDeviceNumber = startOfMobileDeviceLoop + stepOfMobileDeviceLoop * j
                        filePath = ''.join([folderPath, '\ite', str(s + 1), '\SIMRESULT_', str(scenarioType[i]), '_',
                                            orchestratorPolicy, '_', objectPlacement, '_', str(mobileDeviceNumber), 'DEVICES_OBJECTS.log'])
                        data = pd.read_csv(filePath, delimiter=';')
                        host_series = data['HostID'].value_counts().sort_index()
                        access_series = data['AccessID'].value_counts().sort_index()
                        host_frame[orchestratorPolicy] = host_series
                        access_frame[orchestratorPolicy] = access_series
                        # plt.show()
                    placedObjects = pd.DataFrame(host_frame)
                    accessedHosts = pd.DataFrame(access_frame)

                    placedObjects.plot(y=orchestratorPolicies, kind="bar", use_index=True, ax=ax[p])
                    accessedHosts.plot(y=orchestratorPolicies, kind="bar", use_index=True, ax=ax2[p])

                for axis in ax:
                    axis.legend()
                    axis.set_xlabel("Host Number")
                    axis.set_ylabel("Read Objects")
                ax[0].set_title(objectPlacements[0])
                ax[1].set_title(objectPlacements[1])
                ax[2].set_title(objectPlacements[2])
                fig.suptitle("Placed_Objects_" + str(mobileDeviceNumber))
                fig.savefig(folderPath + '\\fig\\Placed_Objects_' + "_" + str(mobileDeviceNumber) + '.png', bbox_inches='tight')
                plt.close(fig)


                for axis in ax2:
                    axis.legend()
                    axis.set_xlabel("Host Number")
                    axis.set_ylabel("Read Objects")
                ax2[0].set_title(objectPlacements[0])
                ax2[1].set_title(objectPlacements[1])
                ax2[2].set_title(objectPlacements[2])
                fig2.suptitle("Accessed_Hosts_" + str(mobileDeviceNumber))
                fig2.savefig(folderPath + '\\fig\\Accessed_Hosts_' + "_" + str(mobileDeviceNumber) + '.png', bbox_inches='tight')
                plt.close(fig2)

# plotReadObjects()
