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

    # fig, ax = plt.subplots(len(objectPlacements), 1,figsize=(12,20))
    fig, ax = plt.subplots(1, 1,figsize=(12,8))
    numOfDevices = list(range(startOfMobileDeviceLoop,endOfMobileDeviceLoop+1,stepOfMobileDeviceLoop))
    latencies = pd.DataFrame(index=numOfDevices)
    costs = pd.DataFrame(index=numOfDevices)
    requests = pd.DataFrame(index=numOfDevices)
    latency_cost_df = pd.DataFrame(columns=["Devices","Latency","Cost","Policy"])
    latency_requests_df = pd.DataFrame(columns=["Devices","Latency","Requests","Policy"])
    latency_index=0
    type_read_total = pd.DataFrame(index=numOfDevices)
    type_read_data = pd.DataFrame(index=numOfDevices)
    type_read_parity = pd.DataFrame(index=numOfDevices)
    markers = ['s', 'x', 'o', '.', ',','^']
    colors = ['#1f77b4', '#ff7f0e', '#2ca02c', '#9467bd','#7f7f7f','#4F0041']
    for p, objectPlacement in enumerate(objectPlacements):
        for o, orchestratorPolicy in enumerate(orchestratorPolicies):
            all_results = np.zeros((numOfSimulations, len(scenarioType), numOfMobileDevices))
            policy = objectPlacement + " | " + orchestratorPolicy
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
                        if (not path.exists(filePath)):
                            break
                        data = pd.read_csv(filePath, delimiter=';')
                        latencies.at[mobileDeviceNumber,policy] = data["latency"].mean()
                        costs.at[mobileDeviceNumber,policy] = data["Read Cost"].mean()
                        requests.at[mobileDeviceNumber,policy] = data.shape[0]
                        type_read_data.at[mobileDeviceNumber,policy] = data[data["type"]=="data"].shape[0]
                        type_read_total.at[mobileDeviceNumber, policy] = data["type"].shape[0]
                        if "parity" in data["type"].unique():
                            type_read_parity.at[mobileDeviceNumber,policy] = data[data["type"]=="parity"].shape[0]
                            # parity_col.at[mobileDeviceNumber, policy] = data[data["type"]=="parity"].shape[0]
                        else:
                            type_read_parity.at[mobileDeviceNumber,policy] = 0
                            # parity_col.at[mobileDeviceNumber, policy] = 0
                            # plt.show()
            if (not latencies.empty):

                # type_read_data[policy] = data_col
                # type_read_parity[policy] = parity_col
                # type_read_total[policy] = total_col
                if policy in latencies:
                    ax.scatter(numOfDevices, latencies[policy], marker=markers[o])
                    ax.plot(numOfDevices, latencies[policy], marker=markers[o], label=policy)
                    # ax.set_title(policy)
    for row in latencies.iterrows():
        for column in latencies.columns:
            # latency_cost_df.loc[-1] = [row[0], latencies.loc[row[0]][column], costs.loc[row[0]][column], column,objectPlacement]
            # latency_cost_df.index = latency_cost_df.index + 1  # shifting index
            # latency_cost_df = latency_cost_df.sort_index()  # sorting by index
            latency_cost_df = latency_cost_df.append({'Devices': row[0], 'Latency': latencies.loc[row[0]][column],
                                  'Cost': costs.loc[row[0]][column], 'Policy': column}, ignore_index=True)
            latency_requests_df = latency_requests_df.append({'Devices': row[0], 'Latency': latencies.loc[row[0]][column],
                                  'Requests': requests.loc[row[0]][column], 'Policy': column}, ignore_index=True)
            # latency_cost_df.loc[latency_index] = [row[0], latencies.loc[row[0]][column], costs.loc[row[0]][column],
            #                                       column,objectPlacement]
            # latency_index+=1

    fig2, ax2 = plt.subplots(3, 1, figsize=(12, 10))
    type_read_total.plot(kind="bar", use_index=True, ax=ax2[0])
    type_read_data.plot(kind="bar", use_index=True, ax=ax2[1])
    type_read_parity.plot(kind="bar", use_index=True, ax=ax2[2])
    for axis in ax2:
        axis.set_xlabel("Devices")
        axis.set_ylabel("Reads")
        axis.grid(True)
    ax2[0].set_title("All Reads")
    ax2[1].set_title("Data Reads")
    ax2[2].set_title("Parity Reads")
    fig2.tight_layout(h_pad=2)
    fig2.savefig(folderPath + '\\fig\\Read_By_Type' + '.png',
                 bbox_inches='tight')

    plt.close(fig2)

    #
    # for axis in ax:
    #     axis.legend()
    #     axis.set_xlabel("Devices")
    #     axis.set_ylabel("Average Data Read Latency[s]")
    #     axis.grid(True)
    ax.legend()
    ax.set_xlabel("Devices")
    ax.set_ylabel("Average Data Read Latency[s]")
    ax.grid(True)

    fig.tight_layout(h_pad=2)
    fig.suptitle("Average Data Read Latency per IO Request", y=1.01)
    fig.savefig(folderPath + '\\fig\\Average Data Read Latency per IO Request' + '.png',
                bbox_inches='tight')
    plt.close(fig)


        # plt.show()
    for count in latency_cost_df["Devices"].unique():
        fig, ax = plt.subplots(1, 1)
        count_df = latency_cost_df[latency_cost_df.Devices==count]
        min_value=1000
        max_value=0

        for index, row in count_df.iterrows():
            x = row["Policy"].split(" | ")
            plt.scatter(x=row["Latency"], y=row["Cost"], color=colors[orchestratorPolicies.index(x[1])],
                        marker=markers[objectPlacements.index(x[0])],alpha=0.7)
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
        ax.set_title("Redundancy Cost vs Latency for " + str(count) + " Devices",y=1.05, fontsize=20)
        # fig.suptitle('test title', fontsize=20)
        fig.tight_layout()
        fig.savefig(folderPath + '\\fig\\Redundancy Cost vs Latency ' + str(count) + ' Devices.png',
                    bbox_inches='tight')
        plt.close(fig)

    for count in latency_requests_df["Devices"].unique():
        fig, ax = plt.subplots(1, 1)
        count_df = latency_requests_df[latency_requests_df.Devices==count]
        min_value=1000
        max_value=0
        for index, row in count_df.iterrows():
            x = row["Policy"].split(" | ")
            plt.scatter(x=row["Latency"], y=row["Requests"], color=colors[orchestratorPolicies.index(x[1])],
                        marker=markers[objectPlacements.index(x[0])],alpha=0.7)
            # min_value=min(min_value,row["Latency"],row["Requests"])
            # max_value=max(max_value,row["Latency"],row["Requests"])
        ax.set_xlabel("Latency")
        ax.set_ylabel("Completed Requests")
        ax.grid(True)
        # x = np.linspace(min_value, max_value, 100)
        # plt.plot(x, x, '-r',alpha=0.3,linewidth=0.5)
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
        ax.set_title("Completed Requests vs Latency for " + str(count) + " Devices",y=1.05, fontsize=20)
        # fig.suptitle('test title', fontsize=20)
        fig.tight_layout()
        fig.savefig(folderPath + '\\fig\\Completed Requests vs Latency ' + str(count) + ' Devices.png',
                    bbox_inches='tight')
        plt.close(fig)



# plotStripesRead()
