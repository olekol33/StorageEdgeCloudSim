from getConfiguration import getConfiguration
import numpy as np
import csv
import matplotlib.pyplot as plt
from scipy.stats import t
import itertools
import pandas as pd
import seaborn as sns

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

def plotStripesRead():
    print("Running " + plotStripesRead.__name__)
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

    fig, ax = plt.subplots(1, 1)
    numOfDevices = list(range(startOfMobileDeviceLoop,endOfMobileDeviceLoop+1,stepOfMobileDeviceLoop))
    latencies = pd.DataFrame(index=numOfDevices, columns=orchestratorPolicies)
    type_read_data = pd.DataFrame(index=numOfDevices, columns=orchestratorPolicies)
    type_read_parity = pd.DataFrame(index=numOfDevices, columns=orchestratorPolicies)
    marker = ['*', 'x', 'o', '.', ',']
    for o, orchestratorPolicy in enumerate(orchestratorPolicies):
        all_results = np.zeros((numOfSimulations, len(scenarioType), numOfMobileDevices))
        # initializing the titles and rows list
        for s in range(numOfSimulations):
            for i in range(len(scenarioType)):
                for j in range(numOfMobileDevices):
                    fields = []
                    rows = []
                    mobileDeviceNumber = startOfMobileDeviceLoop + stepOfMobileDeviceLoop*j
                    filePath = ''.join([folderPath, '\ite', str(s + 1), '\SIMRESULT_', str(scenarioType[i]), '_',
                                        orchestratorPolicy, '_', str(mobileDeviceNumber), 'DEVICES_READOBJECTS.log'])
                    data = pd.read_csv(filePath, delimiter=';')
                    latencies.at[mobileDeviceNumber,orchestratorPolicy] = data["latency"].mean()
                    type_read_data.at[mobileDeviceNumber,orchestratorPolicy] = data["type"].value_counts()["data"]
                    if "parity" in data["type"].unique():
                        type_read_parity.at[mobileDeviceNumber,orchestratorPolicy] = data["type"].value_counts()["parity"]
                    else:
                        type_read_parity.at[mobileDeviceNumber,orchestratorPolicy] = 0
                # plt.show()
                # placedObjects = pd.DataFrame(frame)
        ax.scatter(numOfDevices, latencies[orchestratorPolicy], marker=marker[o])
        ax.plot(numOfDevices, latencies[orchestratorPolicy], marker=marker[o], label=orchestratorPolicy)

    ax.legend()
    ax.set_xlabel("Devices")
    ax.set_ylabel("Average Data Read Latency[s]")
    fig.savefig(folderPath + '\\fig\\Average_Data_Read_Latency' + '.png',
                bbox_inches='tight')
    plt.close(fig)

    fig2, ax2 = plt.subplots(2, 1, figsize=(15,10))
    type_read_data.plot(y=orchestratorPolicies, kind="bar", use_index=True, ax=ax2[0])
    type_read_parity.plot(y=orchestratorPolicies, kind="bar", use_index=True, ax=ax2[1])
    for axis in ax2:
        axis.set_xlabel("Devices")
        axis.set_ylabel("Reads")
    ax2[0].set_title("Data Reads")
    ax2[1].set_title("Parity Reads")
    fig2.tight_layout(h_pad=2)
    fig2.savefig(folderPath + '\\fig\\Read_By_Type' + '.png',
                bbox_inches='tight')

    plt.close(fig2)


