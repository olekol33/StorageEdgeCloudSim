from getConfiguration import getConfiguration
import numpy as np
import csv
import matplotlib.pyplot as plt
from scipy.stats import t
import itertools

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

lanDelay = 0
manDelay = 1
wanDelay = 2
failedTaskDuetoLanBw = 4
failedTaskDuetoManBw = 5
failedTaskDuetoWanBw = 6

def plotGenericResult(rowOfset, columnOfset, yLabel, appType, calculatePercentage):
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
    for p, objectPlacement in enumerate(objectPlacements):
        for o, orchestratorPolicy in enumerate(orchestratorPolicies):
            all_results = np.zeros((numOfSimulations, len(scenarioType), numOfMobileDevices))
            # initializing the titles and rows list
            for s in range(numOfSimulations):
                for i in range(len(scenarioType)):
                    for j in range(numOfMobileDevices):
                        try:
                            fields = []
                            rows = []
                            mobileDeviceNumber = startOfMobileDeviceLoop + stepOfMobileDeviceLoop*j
                            filePath = ''.join([folderPath, '\ite', str(s+1), '\SIMRESULT_', str(scenarioType[i]), '_',
                                                orchestratorPolicy,'_',objectPlacement,'_',str(mobileDeviceNumber), 'DEVICES_', appType, '_GENERIC.log'])
                            with open(filePath,'r') as csvfile:
                                csvreader = csv.reader(csvfile, delimiter=';')
                                for row in csvreader:
                                    if row[0][0] == '#':
                                        fields.append(row)
                                    else:
                                        rows.append(row)
        #                        value = [row for idx, row in enumerate(rows) if idx in (rowOfset, columnOfset)]
                                value = float(rows[rowOfset][columnOfset])
                                if calculatePercentage == 'percentage_for_all':
        #                            readData = [row for idx, row in enumerate(rows) if idx in (1, 0)]
        #                            totalTask = readData[0][0] + readData[0][1]
                                    totalTask = float(rows[TotalTasks][completedTask]) + int(rows[TotalTasks][failedTask])
                                    value = (100 * value) / totalTask

                                elif calculatePercentage == 'percentage_for_completed':
        #                            readData = [row for idx, row in enumerate(rows) if idx in (1, 0)]
                                    totalTask = float(rows[TotalTasks][completedTask])
                                    value = (100 * value) / totalTask;
                                elif calculatePercentage == 'percentage_for_failed':
        #                            readData = [row for idx, row in enumerate(rows) if idx in (1, 0)]
                                    totalTask = float(rows[TotalTasks][failedTask])
                                    # print(orchestratorPolicy+str(mobileDeviceNumber))
                                    value = (100 * value) / totalTask
                                all_results[s, i, j] = value
                        except FileNotFoundError:
                            print("The following file doesn't exist:\n" + filePath)

            if numOfSimulations == 1:
                results = all_results
            else:
                results = np.mean(all_results)

            results = np.squeeze(results)
            if len(scenarioType) > 1:
                min_results = np.zeros((len(scenarioType), numOfMobileDevices))
                max_results = np.zeros((len(scenarioType), numOfMobileDevices))
                for i in range(len(scenarioType)):
                    for j in range(numOfMobileDevices):
                        x = all_results[:, i, j] #Create Data
                        SEM = np.std(x) / np.sqrt(len(x)) #Standard Error
                        # Student's t inverse cumulative distribution function
                        df = len(x)-1  #degrees of freedom
                        ts = np.linspace(t.ppf(0.05, df), t.ppf(0.95, df), 2)
                        CI = np.mean(x) + ts * SEM; #Confidence Intervals (if len(x)>1)
                        if CI[0] < 0:
                            CI[0] = 0
                        if CI[1] < 0:
                            CI[1] = 0

                        min_results[i, j] = results[i][j] - CI[0]
                        max_results[i, j] = CI[1] - results[i][j]
            else:
                for j in range(numOfMobileDevices):
                    min_results = np.zeros((numOfMobileDevices))
                    max_results = np.zeros((numOfMobileDevices))
                    x = all_results[:, :, j]  # Create Data
                    SEM = np.std(x) / np.sqrt(len(x))  # Standard Error
                    # Student's t inverse cumulative distribution function
                    df = len(x) - 1  # degrees of freedom
                    ts = np.linspace(t.ppf(0.05, df), t.ppf(0.95, df), 2)
                    CI = np.mean(x) + ts * SEM;  # Confidence Intervals (if len(x)>1)
                    if CI[0] < 0:
                        CI[0] = 0
                    if CI[1] < 0:
                        CI[1] = 0

                    min_results[j] = results[j] - CI[0]
                    max_results[j] = CI[1] - results[j]

                types = np.zeros([1,numOfMobileDevices])
                marker = ['*', 'x', 'o', '.', ',']
                colors = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd']
                xIndex = []
                for i in range(numOfMobileDevices):
                    types[0][i]=startOfMobileDeviceLoop+(i*stepOfMobileDeviceLoop)
                    xIndex.append(startOfMobileDeviceLoop + (i* stepOfMobileDeviceLoop))

                for j,scen in enumerate(scenarioType):
                    yIndex = []
                    for i in range(numOfMobileDevices):
                        # TODO: temp removed
                        # yIndex.append(results[j][i])
                        yIndex.append(results[i])
                    ax[p].scatter(xIndex, yIndex, marker = marker[o])
                    ax[p].plot(xIndex, yIndex, marker = marker[j], label=orchestratorPolicy)

    for axis in ax:
        axis.legend()
        axis.set_xlabel("Number of Mobile Devices")
        axis.set_ylabel(yLabel)
    ax[0].set_title(objectPlacements[0])
    ax[1].set_title(objectPlacements[1])
    ax[2].set_title(objectPlacements[2])

    # ax.legend()
    # ax.set_xlabel("Number of Mobile Devices")
    # ax.set_ylabel(yLabel)
    fig.savefig(folderPath + '\\fig\\' + yLabel+ '.png', bbox_inches='tight')
    plt.close(fig)



