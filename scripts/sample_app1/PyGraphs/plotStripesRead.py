from getConfiguration import getConfiguration
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
    objectPlacements = getConfiguration("objectPlacement");
    numOfMobileDevices = int((endOfMobileDeviceLoop - startOfMobileDeviceLoop) / stepOfMobileDeviceLoop + 1)
#    pos = getConfiguration(9);

    fig, ax = plt.subplots(len(objectPlacements), 1,figsize=(10,20))
    numOfDevices = list(range(startOfMobileDeviceLoop,endOfMobileDeviceLoop+1,stepOfMobileDeviceLoop))
    latencies = pd.DataFrame(index=numOfDevices, columns=orchestratorPolicies)
    costs = pd.DataFrame(index=numOfDevices, columns=orchestratorPolicies)
    latency_cost_df = pd.DataFrame(columns=["Devices","Latency","Cost","Orchestrator Policy","Placement"])
    latency_index=0
    type_read_data = pd.DataFrame(index=numOfDevices, columns=orchestratorPolicies)
    type_read_parity = pd.DataFrame(index=numOfDevices, columns=orchestratorPolicies)
    markers = ['s', 'x', 'o', '.', ',']
    colors = ['#1f77b4', '#ff7f0e', '#2ca02c', '#9467bd','#7f7f7f']
    for p, objectPlacement in enumerate(objectPlacements):
        for o, orchestratorPolicy in enumerate(orchestratorPolicies):
            all_results = np.zeros((numOfSimulations, len(scenarioType), numOfMobileDevices))
            # initializing the titles and rows list
            for s in range(numOfSimulations):
                for i in range(len(scenarioType)):
                    if (scenarioType[i] == "TWO_TIER" and not "CLOUD" in orchestratorPolicy):
                        continue
                    elif (scenarioType[i] != "TWO_TIER" and "CLOUD" in orchestratorPolicy):
                        continue
                    for j in range(numOfMobileDevices):
                        fields = []
                        rows = []
                        mobileDeviceNumber = startOfMobileDeviceLoop + stepOfMobileDeviceLoop*j
                        filePath = ''.join([folderPath, '\ite', str(s + 1), '\SIMRESULT_', str(scenarioType[i]), '_',
                                            orchestratorPolicy, '_', objectPlacement, '_', str(mobileDeviceNumber), 'DEVICES_READOBJECTS.log'])
                        data = pd.read_csv(filePath, delimiter=';')
                        latencies.at[mobileDeviceNumber,orchestratorPolicy] = data["latency"].mean()
                        costs.at[mobileDeviceNumber,orchestratorPolicy] = data["Read Cost"].mean()
                        type_read_data.at[mobileDeviceNumber,orchestratorPolicy] = data["type"].value_counts()["data"]
                        if "parity" in data["type"].unique():
                            type_read_parity.at[mobileDeviceNumber,orchestratorPolicy] = data["type"].value_counts()["parity"]
                        else:
                            type_read_parity.at[mobileDeviceNumber,orchestratorPolicy] = 0
                    # plt.show()
                    # placedObjects = pd.DataFrame(frame)
            ax[p].scatter(numOfDevices, latencies[orchestratorPolicy], marker=markers[o])
            ax[p].plot(numOfDevices, latencies[orchestratorPolicy], marker=markers[o], label=orchestratorPolicy)
        for row in latencies.iterrows():
            for column in latencies.columns:
                # latency_cost_df.loc[-1] = [row[0], latencies.loc[row[0]][column], costs.loc[row[0]][column], column,objectPlacement]
                # latency_cost_df.index = latency_cost_df.index + 1  # shifting index
                # latency_cost_df = latency_cost_df.sort_index()  # sorting by index
                latency_cost_df = latency_cost_df.append({'Devices': row[0], 'Latency': latencies.loc[row[0]][column],
                                      'Cost': costs.loc[row[0]][column], 'Orchestrator Policy': column,
                                      'Placement':objectPlacement}, ignore_index=True)
                # latency_cost_df.loc[latency_index] = [row[0], latencies.loc[row[0]][column], costs.loc[row[0]][column],
                #                                       column,objectPlacement]
                # latency_index+=1

        fig2, ax2 = plt.subplots(2, 1, figsize=(15, 10))
        type_read_data.plot(y=orchestratorPolicies, kind="bar", use_index=True, ax=ax2[0])
        type_read_parity.plot(y=orchestratorPolicies, kind="bar", use_index=True, ax=ax2[1])
        for axis in ax2:
            axis.set_xlabel("Devices")
            axis.set_ylabel("Reads")
        ax2[0].set_title("Data Reads")
        ax2[1].set_title("Parity Reads")
        fig2.tight_layout(h_pad=2)
        fig2.savefig(folderPath + '\\fig\\Read_By_Type' + "_" + objectPlacement + '.png',
                     bbox_inches='tight')

        plt.close(fig2)


    for axis in ax:
        axis.legend()
        axis.set_xlabel("Devices")
        axis.set_ylabel("Average Data Read Latency[s]")
        axis.grid(True)
    ax[0].set_title(objectPlacements[0])
    ax[1].set_title(objectPlacements[1])
    ax[2].set_title(objectPlacements[2])

    fig.tight_layout(h_pad=2)
    fig.savefig(folderPath + '\\fig\\Average_Data_Read_Latency' + '.png',
                bbox_inches='tight')
    plt.close(fig)


        # plt.show()

    for count in latency_cost_df["Devices"].unique():
        fig, ax = plt.subplots(1, 1)
        count_df = latency_cost_df[latency_cost_df.Devices==count]
        min_value=1000
        max_value=0
        for index, row in count_df.iterrows():
            plt.scatter(x=row["Latency"], y=row["Cost"], color=colors[orchestratorPolicies.index(row["Orchestrator Policy"])],
                        marker=markers[objectPlacements.index(row["Placement"])],alpha=0.7)
            min_value=min(min_value,row["Latency"],row["Cost"])
            max_value=max(max_value,row["Latency"],row["Cost"])
        ax.set_xlabel("Latency")
        ax.set_ylabel("Cost")
        ax.grid(True)
        x = np.linspace(min_value, max_value, 100)
        plt.plot(x, x, '-r',alpha=0.3,linewidth=0.5)
        # legend1 = ax.legend(*scatter.legend_elements(num=5),
        #                     loc="upper left", title="Ranking")
        # ax.add_artist(legend1)
        # legend2 = ax.legend(*scatter.legend_elements(**kw),
        #                     loc="lower right", title="Price")
        # ax.legend(markers[0:len(objectPlacements)], objectPlacements)
        f = lambda m, c: plt.plot([], [], marker=m, color=c, ls="none")[0]
        handles = [f("s", colors[i]) for i in range(len(orchestratorPolicies))]
        handles += [f(markers[i], "k") for i in range(len(objectPlacements))]

        labels = orchestratorPolicies + objectPlacements

        ax.legend(handles, labels, loc=0, framealpha=0.6,prop={'size': 8})
        ax.set_title("Cost vs Latency for " + str(count) + " Devices",y=1.05, fontsize=20)
        # fig.suptitle('test title', fontsize=20)
        fig.tight_layout()
        fig.savefig(folderPath + '\\fig\\Cost vs Latency ' + str(count) + ' Devices.png',
                    bbox_inches='tight')
        plt.close(fig)

plotStripesRead()
