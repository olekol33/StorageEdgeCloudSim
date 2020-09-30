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
        for j in range(numOfMobileDevices):
            host_frame = {}
            access_frame = {}
            delay_frame = {}
            fig, ax = plt.subplots(len(objectPlacements), 1, figsize=(12, 17))
            fig2, ax2 = plt.subplots(len(objectPlacements), 1, figsize=(12, 17))
            fig3, ax3 = plt.subplots(len(objectPlacements), 1, figsize=(12, 17))
            for p, objectPlacement in enumerate(objectPlacements):
                for i in range(len(scenarioType)):
                    for o, orchestratorPolicy in enumerate(orchestratorPolicies):
                        if (scenarioType[i] == "TWO_TIER" and not "CLOUD" in orchestratorPolicy):
                            continue
                        elif (scenarioType[i] != "TWO_TIER" and "CLOUD" in orchestratorPolicy):
                            continue
                        mobileDeviceNumber = startOfMobileDeviceLoop + stepOfMobileDeviceLoop * j
                        filePath = ''.join([folderPath, '\ite', str(s + 1), '\SIMRESULT_', str(scenarioType[i]), '_',
                                            orchestratorPolicy, '_', objectPlacement, '_', str(mobileDeviceNumber), 'DEVICES_OBJECTS.log'])
                        if (not path.exists(filePath)):
                            continue
                        data = pd.read_csv(filePath, delimiter=';')
                        host_series = data['HostID'].value_counts().sort_index()
                        access_series = data['AccessID'].value_counts().sort_index()
                        # delay_series = data['Read Delay'].value_counts().sort_index()
                        delay_series = data.groupby(['AccessID']).mean()['Read Delay']
                        host_frame[orchestratorPolicy] = host_series
                        access_frame[orchestratorPolicy] = access_series
                        delay_frame[orchestratorPolicy] = delay_series
                        # plt.show()
                placedObjects = pd.DataFrame(host_frame).fillna(0)
                accessedHosts = pd.DataFrame(access_frame).fillna(0)
                objectReadDelay = pd.DataFrame(delay_frame).fillna(0)

                placedObjects.plot(y=orchestratorPolicies, kind="bar", use_index=True, ax=ax[p])
                accessedHosts.plot(y=orchestratorPolicies, kind="bar", use_index=True, ax=ax2[p])
                objectReadDelay.plot(y=orchestratorPolicies, kind="bar", use_index=True, ax=ax3[p])
                ax[p].set_title(objectPlacements[p])
                ax2[p].set_title(objectPlacements[p])
                ax3[p].set_title(objectPlacements[p])
                ax[p].legend()
                ax[p].set_xlabel("Host Number")
                ax[p].set_ylabel("Read Objects")
                ax2[p].legend()
                ax2[p].set_xlabel("Host Number")
                ax2[p].set_ylabel("Read Objects")
                ax3[p].legend()
                ax3[p].set_xlabel("Host Number")
                ax3[p].set_ylabel("Average Delay")

            # for axis in ax:
            #     axis.legend()
            #     axis.set_xlabel("Host Number")
            #     axis.set_ylabel("Read Objects")
            fig.suptitle("Placed_Objects_" + str(mobileDeviceNumber))
            fig.savefig(folderPath + '\\fig\\Placed_Objects_' + "_" + str(mobileDeviceNumber) + '.png', bbox_inches='tight')
            plt.close(fig)


            # for axis in ax2:
            #     axis.legend()
            #     axis.set_xlabel("Host Number")
            #     axis.set_ylabel("Read Objects")
            fig2.suptitle("Accessed_Hosts_" + str(mobileDeviceNumber))
            fig2.savefig(folderPath + '\\fig\\Accessed_Hosts_' + "_" + str(mobileDeviceNumber) + '.png', bbox_inches='tight')
            plt.close(fig2)

            fig3.suptitle("Average Delay In Accessed Host " + str(mobileDeviceNumber))
            fig3.savefig(folderPath + '\\fig\\Average_Delay_in_Accessed_Hosts_' + "_" + str(mobileDeviceNumber) + '.png', bbox_inches='tight')
            plt.close(fig3)

    for s in range(numOfSimulations):
        for i in range(len(scenarioType)):
            for o, orchestratorPolicy in enumerate(orchestratorPolicies):
                for j in range(numOfMobileDevices):
                    fig, ax = plt.subplots(len(objectPlacements), 1, figsize=(15, 17))
                    fig2, ax2 = plt.subplots(len(objectPlacements), 2, figsize=(15, 17), sharey=True)
                    objects_df = pd.DataFrame(columns=["Host", "Type", "Value", "Value Type", "Policy"])
                    for p, objectPlacement in enumerate(objectPlacements):
                        width = 0.35
                        if (scenarioType[i] == "TWO_TIER" and not "CLOUD" in orchestratorPolicy):
                            continue
                        elif (scenarioType[i] != "TWO_TIER" and "CLOUD" in orchestratorPolicy):
                            continue
                        mobileDeviceNumber = startOfMobileDeviceLoop + stepOfMobileDeviceLoop * j
                        filePath = ''.join([folderPath, '\ite', str(s + 1), '\SIMRESULT_', str(scenarioType[i]), '_',
                                            orchestratorPolicy, '_', objectPlacement, '_', str(mobileDeviceNumber),
                                            'DEVICES_OBJECTS_IN_HOSTS.log'])
                        if (not path.exists(filePath)):
                            continue
                        data = pd.read_csv(filePath, delimiter=';')
                        data.plot.bar(x='host', y='dataObjects', ax=ax[p], stacked=True, label="Data", color='green')
                        data.plot.bar(x='host', y='parityObjects',bottom=data.dataObjects,
                                                          ax=ax[p], stacked=True,label="Parity", color='blue')
                        ax[p].set_title(objectPlacements[p])
                        ax[p].legend()
                        ax[p].set_xlabel("Host Number")
                        ax[p].set_ylabel("Objects in Hosts")
                        # ax[p] = plt.figure().gca()
                        # ax[p].xaxis.set_major_locator(MaxNLocator(integer=True))


                        for host in data["host"].unique():
                            host_data = data[data["host"] == host]
                            # df2 = pd.DataFrame([[host, "data", host_data["meanData"].values[0], host_data["medianData"].values[0]]], columns=["host","type","mean","median"])
                            objects_df.loc[-1] = [host, "data", host_data["meanData"].values[0],"mean",objectPlacement]
                            objects_df.index = objects_df.index + 1  # shifting index
                            objects_df = objects_df.sort_index()  # sorting by index
                            objects_df.loc[-1] = [host, "data", host_data["medianData"].values[0],"median",objectPlacement]
                            objects_df.index = objects_df.index + 1  # shifting index
                            objects_df = objects_df.sort_index()  # sorting by index
                            objects_df.loc[-1] = [host, "parity", host_data["meanParity"].values[0],"mean",objectPlacement]
                            objects_df.index = objects_df.index + 1  # shifting index
                            objects_df = objects_df.sort_index()  # sorting by index
                            objects_df.loc[-1] = [host, "parity", host_data["medianParity"].values[0],"median",objectPlacement]
                            objects_df.index = objects_df.index + 1  # shifting index
                            objects_df = objects_df.sort_index()  # sorting by index

                        filePath = ''.join([folderPath, '\ite', str(s + 1), '\SIMRESULT_', str(scenarioType[i]), '_',
                                            orchestratorPolicy, '_', objectPlacement, '_', str(mobileDeviceNumber),
                                            'DEVICES_OBJECT_DISTRIBUTION.log'])
                        data = pd.read_csv(filePath, delimiter=';')
                        df = data[data["Object Type"] == 'data']
                        df = df.reset_index()
                        df.plot.bar(y='Occurrences', ax=ax2[p][0], use_index=True, color='green',
                                    xticks=list(range(0,max(df.index.values)+2,5)))
                        df = data[data["Object Type"] == 'parity']
                        df = df.reset_index()
                        if(df.shape[0]>0):
                            df.plot.bar(y='Occurrences', ax=ax2[p][1], use_index=True, color='blue',xticks=list(range(0,max(df.index.values)+2,5)))
                    for ax, col in zip(ax2[0], ["Data","Parity"]):
                        ax.set_title(col)
                        ax.legend().set_visible(False)

                    for ax, row in zip(ax2[:, 0], objectPlacements):
                        ax.set_ylabel(row + "\nHosts", rotation=90, size='large')
                        ax.legend().set_visible(False)
                    ax2[2][1].legend().set_visible(False)
                    fig2.tight_layout()
                    fig2.suptitle("Object Distribution By Hosts",y=1.01)
                    fig2.savefig(folderPath + '\\fig\\Object_Distribution_By_Hosts_' + '.png', bbox_inches='tight')
                    plt.close(fig2)

                    # fig.suptitle("Placed Objects By Type" + str(mobileDeviceNumber))
                    # fig.savefig(folderPath + '\\fig\\Object_Popularity_' + '.png', bbox_inches='tight')
                    # plt.close(fig)

                    fig.suptitle("Object Distribution By Type" + str(mobileDeviceNumber))
                    fig.savefig(folderPath + '\\fig\\Object_Distribution_By_Object_' + '.png', bbox_inches='tight')
                    plt.close(fig2)


                    objects_df.fillna(0);
                    g=sns.catplot(data=objects_df, x='Host', y='Value', hue='Type',row='Policy', col="Value Type",
                                kind='bar',height=3, aspect=0.8,margin_titles=True,legend=False)
                    plt.legend(loc='upper left')
                    g.savefig(folderPath + '\\fig\\Object_Distribution_By_Host_' + '.png', bbox_inches='tight')
                    plt.close()
                    exit()




# plotReadObjects()
