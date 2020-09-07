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

def plotHostQueue():
    print("Running " + plotHostQueue.__name__)
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


    numOfDevices = list(range(startOfMobileDeviceLoop,endOfMobileDeviceLoop+1,stepOfMobileDeviceLoop))
    latencies = pd.DataFrame(index=numOfDevices, columns=orchestratorPolicies)
    marker = ['*', 'x', 'o', '.', ',']
    # sns.set_palette(sns.color_palette("Dark2", 20))
    NUM_COLORS = 11
    cm = plt.get_cmap('gist_rainbow')


    # initializing the titles and rows list
    for s in range(numOfSimulations):
        for i in range(len(scenarioType)):
            for j in range(numOfMobileDevices):
                mobileDeviceNumber = startOfMobileDeviceLoop + stepOfMobileDeviceLoop * j

                for o, orchestratorPolicy in enumerate(orchestratorPolicies):
                    c = 0
                    fig, ax = plt.subplots(1, 1)
                    filePath = ''.join([folderPath, '\ite', str(s + 1), '\SIMRESULT_', str(scenarioType[i]), '_',
                                        orchestratorPolicy, '_', str(mobileDeviceNumber), 'DEVICES_HOST_QUEUE.log'])
                    data = pd.read_csv(filePath, delimiter=';')
                    exists = False
                    for host in data["HostID"].unique():
                        host_data = data[data["HostID"] == host]
                        # plot only above threshold
                        if host_data["Requests"].max()>10:
                            exists = True
                            # host_data = data[data["HostID"]==host]
                            sns.lineplot(x="Time",y="Requests",data=host_data,label=host,color=cm(1. * c / NUM_COLORS))
                        c += 1

                    if exists==False:
                        plt.close(fig)
                        continue
                    # ax.legend()
                    ax.set_xlabel("Time")
                    ax.set_ylabel("Queue Size")
                    fig.savefig(folderPath + '\\fig\\HOST_QUEUE_' + orchestratorPolicy + "_" +
                                str(mobileDeviceNumber)+ 'Devices.png',bbox_inches='tight')
                    plt.close(fig)




