from getConfiguration import *
import numpy as np
import csv
import matplotlib.pyplot as plt
from scipy.stats import t
import itertools
import pandas as pd
import seaborn as sns
from os import path

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
    objectPlacements = getConfiguration("objectPlacement");
    numOfMobileDevices = int((endOfMobileDeviceLoop - startOfMobileDeviceLoop) / stepOfMobileDeviceLoop + 1)
#    pos = getConfiguration(9);


    numOfDevices = list(range(startOfMobileDeviceLoop,endOfMobileDeviceLoop+1,stepOfMobileDeviceLoop))
    latencies = pd.DataFrame(index=numOfDevices, columns=orchestratorPolicies)
    marker = ['*', 'x', 'o', '.', ',']
    # sns.set_palette(sns.color_palette("Dark2", 20))

    cm = plt.get_cmap('gist_rainbow')


    # initializing the titles and rows list
    for s in range(numOfSimulations):
        for j in range(numOfMobileDevices):
            queue_size_frame = {}
            mobileDeviceNumber = startOfMobileDeviceLoop + stepOfMobileDeviceLoop * j

            fig2, ax2 = plt.subplots(1, 1, figsize=(17, 12))
            for p, objectPlacement in enumerate(objectPlacements):
                for i in range(len(scenarioType)):
                    for o, orchestratorPolicy in enumerate(orchestratorPolicies):
                        if (scenarioType[i] == "TWO_TIER" and not "CLOUD" in orchestratorPolicy):
                            continue
                        elif (scenarioType[i] != "TWO_TIER" and "CLOUD" in orchestratorPolicy):
                            continue
                        c = 0
                        policy = objectPlacement + " | " + orchestratorPolicy

                        filePath = ''.join([folderPath, '\ite', str(s + 1), '\SIMRESULT_', str(scenarioType[i]), '_',
                                            orchestratorPolicy, '_', objectPlacement, '_',str(mobileDeviceNumber), 'DEVICES_HOST_QUEUE.log'])
                        if (not path.exists(filePath)):
                            continue
                        fig, ax = plt.subplots(1, 1, figsize=(20,10))
                        data = pd.read_csv(filePath, delimiter=';')
                        NUM_COLORS = max(data["HostID"])
                        exists = False
                        queue_size_series = data.groupby(['HostID']).mean()["Requests"]
                        # queue_size_frame[orchestratorPolicy] = queue_size_series
                        queue_size_frame[policy] = queue_size_series
                        if data["Requests"].max()>30:
                        # if mobileDeviceNumber==1000:
                            exists = True
                            for host in data["HostID"].unique():
                                host_data = data[data["HostID"] == host]
                                # plot only above threshold
                                # if host_data["Requests"].max()>30:
                                    # exists = True
                                    # host_data = data[data["HostID"]==host]
                                sns.lineplot(x="Time",y="Requests",data=host_data,label=host,color=cm(1. * c / NUM_COLORS))
                                c += 1

                        if exists==False:
                            plt.close(fig)
                            continue
                        # ax.legend()
                        ax.set_xlabel("Time")
                        ax.set_ylabel("Queue Size")
                        fig.suptitle('HOST WLAN QUEUE - ' + orchestratorPolicy + "; " + objectPlacement + "; " +
                                    str(mobileDeviceNumber)+ 'Devices'+ "; " + getConfiguration("runType"))
                        fig.savefig(folderPath + '\\fig\\HOST_WLAN_QUEUE_' + orchestratorPolicy + "_" + objectPlacement + "_" +
                                    str(mobileDeviceNumber)+ ' Devices.png',bbox_inches='tight')
                        plt.close(fig)

            queue_size_df = pd.DataFrame(queue_size_frame).fillna(0)
            queue_size_df.plot(kind="bar", use_index=True, ax=ax2)
            # ax2.set_title(objectPlacements[p])
            ax2.legend()
            ax2.set_xlabel("Host Number")
            ax2.set_ylabel("Average Queue Size")
            fig2.suptitle("Average Queue Size " + str(mobileDeviceNumber)+ " - " + getConfiguration("runType"))
            fig2.savefig(folderPath + '\\fig\\Average_WLAN_Queue_Size' + "_" + str(mobileDeviceNumber) + '.png',
                        bbox_inches='tight')
            plt.close(fig2)

    # initializing the titles and rows list
    for s in range(numOfSimulations):
        for j in range(numOfMobileDevices):
            queue_size_frame = {}
            mobileDeviceNumber = startOfMobileDeviceLoop + stepOfMobileDeviceLoop * j

            fig2, ax2 = plt.subplots(1, 1, figsize=(17, 14))
            for p, objectPlacement in enumerate(objectPlacements):
                for i in range(len(scenarioType)):
                    for o, orchestratorPolicy in enumerate(orchestratorPolicies):
                        if (scenarioType[i] == "TWO_TIER" and not "CLOUD" in orchestratorPolicy):
                            continue
                        elif (scenarioType[i] != "TWO_TIER" and "CLOUD" in orchestratorPolicy):
                            continue
                        c = 0
                        policy = objectPlacement + " | " + orchestratorPolicy
                        filePath = ''.join([folderPath, '\ite', str(s + 1), '\SIMRESULT_', str(scenarioType[i]), '_',
                                            orchestratorPolicy, '_', objectPlacement, '_',str(mobileDeviceNumber), 'DEVICES_MAN_QUEUE.log'])
                        if (not path.exists(filePath)):
                            continue
                        fig, ax = plt.subplots(1, 1, figsize=(20,10))
                        data = pd.read_csv(filePath, delimiter=';')
                        NUM_COLORS = max(data["HostID"])
                        exists = False
                        queue_size_series = data.groupby(['HostID']).mean()["Requests"]
                        queue_size_frame[policy] = queue_size_series
                        if data["Requests"].max()>20:
                        # if mobileDeviceNumber==1000:
                            exists = True
                            for host in data["HostID"].unique():
                                host_data = data[data["HostID"] == host]
                                # plot only above threshold
                                # if host_data["Requests"].max()>30:
                                    # exists = True
                                    # host_data = data[data["HostID"]==host]
                                sns.lineplot(x="Time",y="Requests",data=host_data,label=host,color=cm(1. * c / NUM_COLORS))
                                c += 1

                        if exists==False:
                            plt.close(fig)
                            continue
                        # ax.legend()
                        # ax.set_title('MAN Queue - Orchestrator Policy: ' + orchestratorPolicy + ", Object Placement: " +
                        #              objectPlacement + ", Devices: " + str(mobileDeviceNumber)+ " - " + getConfiguration("runType"))
                        ax.set_xlabel("Time")
                        ax.set_ylabel("Queue Size")
                        fig.suptitle('HOST MAN QUEUE - ' + orchestratorPolicy + "; " + objectPlacement + "; " +
                                    str(mobileDeviceNumber)+ ' Devices'+ "; " + getConfiguration("runType"))
                        fig.savefig(folderPath + '\\fig\\HOST_MAN_QUEUE_' + orchestratorPolicy + "_" + objectPlacement + "_" +
                                    str(mobileDeviceNumber)+ 'Devices.png',bbox_inches='tight')
                        plt.close(fig)

            queue_size_df = pd.DataFrame(queue_size_frame).fillna(0)
            colNames = []
            for index, columns in queue_size_df.iteritems():
                colNames.append(renamePolicy(index))
            queue_size_df.columns = colNames

            # queue_size_df.plot(kind="bar", use_index=True, ax=ax2)
            ind_list = [4,5,6,7,8,9]
            queue_size_df.iloc[ind_list].plot(kind="bar", use_index=True, ax=ax2)
            ax2.legend(framealpha=0.6,prop={'size': 30},loc='upper center', bbox_to_anchor=(0.4, 0.5, 0.5, 0.5))
            ax2.set_xlabel("Node Number", fontsize=38)
            ax2.set_ylabel("Average Queue Size", fontsize=38)
            ax2.tick_params(labelsize=30)

            ax2.set_ylim([0, 50])
            start, end = ax2.get_ylim()
            ax2.yaxis.set_ticks(np.arange(start, end, 5))
            ax2.xaxis.set_tick_params(rotation=0)
            fig2.tight_layout()
            # fig2.suptitle("Average Queue Size " + str(mobileDeviceNumber)+ " - " + getConfiguration("runType"))
            fig2.savefig(folderPath + '\\fig\\Average_MAN_Queue_Size' + "_" + str(mobileDeviceNumber) + '.png',
                        bbox_inches='tight')
            plt.close(fig2)



plotHostQueue()
