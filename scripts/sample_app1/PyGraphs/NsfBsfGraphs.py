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
import re
from os import listdir
from os.path import isfile, join

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
failedTaskDuetoInaccessibility = 13

lanDelay = 0
manDelay = 1
wanDelay = 2
failedTaskDuetoLanBw = 4
failedTaskDuetoManBw = 5
failedTaskDuetoWanBw = 6

def Filter(list, subs):
    filter_data = [i for i in list if subs in i]
    return filter_data

def get_extra_object_reads(lambdas,failed_lambdas,files,folderPath):
    extra_objects_read = pd.DataFrame()
    for lam in lambdas:
        if lam in failed_lambdas:
            continue
        lambda0 = lam[0][0]
        lambda1 = lam[0][1]
        fileName = Filter(files, ''.join(['SIMRESULT_',lambda0,'_',lambda1]))
        filePath = ''.join([folderPath, '\ite1\\',fileName[0]])
        data = pd.read_csv(filePath, delimiter=';')
        print(lambda0,lambda1)
        extra_objects_read.at[lambda0,lambda1] = data["Objects Read"].sum() - data["Objects Read"].count()

    extra_objects_read.reset_index(inplace=True)
    extra_objects_read_index=extra_objects_read["index"].astype(float)
    extra_objects_read_index=round(extra_objects_read_index,4)
    extra_objects_read=extra_objects_read.iloc[:, 1:]

    extra_objects_read = pd.DataFrame(np.vstack([extra_objects_read.columns, extra_objects_read]))
    extra_objects_read = extra_objects_read.astype(float)
    extra_objects_read.sort_values(by=0, ascending=True, axis=1,inplace=True)
    new_header = extra_objects_read.iloc[0]
    extra_objects_read = extra_objects_read[1:]
    new_header = round(new_header,4)
    extra_objects_read.columns = new_header
    extra_objects_read.reset_index(inplace=True)

    extra_objects_read["index"] = extra_objects_read_index
    extra_objects_read.sort_values(by=["index"], ascending=False,inplace=True)
    extra_objects_read.set_index("index",inplace=True)
    # extra_objects_read = extra_objects_read.rename(columns={'index': 'new column name'})
    # df = extra_objects_read.rename_axis('MyIdx').sort_values(by=['MyIdx'])

    return extra_objects_read

def read_by_type(lambdas,files,folderPath):
    type_read_total = pd.DataFrame(columns=["lambdas","count"])
    type_read_data = pd.DataFrame(columns=["lambdas","count"])
    type_read_parity = pd.DataFrame(columns=["lambdas","count"])
    for lam in lambdas:
        lambda0 = lam[0][0]
        lambda1 = lam[0][1]
        fileName = Filter(files, ''.join(['SIMRESULT_',lambda0,'_',lambda1]))
        filePath = ''.join([folderPath, '\ite1\\',fileName[0]])
        data = pd.read_csv(filePath, delimiter=';')
        # type_read_data.at[mobileDeviceNumber, policy] = data[data["type"] == "data"].shape[0]
        type_read_data.loc[len(type_read_data.index)] = [lam[0],data[data["type"] == "data"].shape[0]]
        type_read_total.loc[len(type_read_total.index)] = [lam[0],data["type"].shape[0]]
        # type_read_total.at[mobileDeviceNumber, policy] = data["type"].shape[0]
        if "parity" in data["type"].unique():
            # type_read_parity.at[mobileDeviceNumber, policy] = data[data["type"] == "parity"].shape[0]
            type_read_parity.loc[len(type_read_parity.index)] = [lam[0], data[data["type"] == "parity"].shape[0]]

            # parity_col.at[mobileDeviceNumber, policy] = data[data["type"]=="parity"].shape[0]
        else:
            # type_read_parity.at[mobileDeviceNumber, policy] = 0
            type_read_parity.loc[len(type_read_parity.index)] = [lam[0], 0]
            # parity_col.at[mobileDeviceNumber, policy] = 0
            # plt.show()
    fig2, ax2 = plt.subplots(3, 1, figsize=(25, 15))
    type_read_total.plot(x="lambdas", y="count",kind="bar", use_index=True, ax=ax2[0])
    type_read_data.plot(x="lambdas", y="count",kind="bar", use_index=True, ax=ax2[1])
    type_read_parity.plot(x="lambdas", y="count",kind="bar", use_index=True, ax=ax2[2])
    for axis in ax2:
        axis.set_xlabel("Lambdas")
        axis.set_ylabel("Reads")
        axis.grid(True)
    ax2[0].set_title("All Reads")
    ax2[1].set_title("Data Reads")
    ax2[2].set_title("Parity Reads")
    fig2.tight_layout(h_pad=2)
    fig2.savefig(folderPath + '\\fig\\Read_By_Type' + '.png',
                 bbox_inches='tight')

    plt.close(fig2)

def failed_tasks(lambdas,files,folderPath,extra_objects_read):
    for lam in lambdas:
        lambda0 = lam[0][0]
        lambda1 = lam[0][1]
        fileName = Filter(files, ''.join(['SIMRESULT_',lambda0,'_',lambda1]))
        filePath = ''.join([folderPath, '\ite1\\',fileName[0]])
        fields = []
        rows = []
        data = pd.read_csv(filePath, delimiter=';')
        with open(filePath,'r') as csvfile:
            csvreader = csv.reader(csvfile, delimiter=';')
            for row in csvreader:
                if row[0][0] == '#':
                    fields.append(row)
                else:
                    rows.append(row)
#                        value = [row for idx, row in enumerate(rows) if idx in (rowOfset, columnOfset)]
            if(float(rows[TotalTasks][failedTask]) > float(rows[TotalTasks][failedTaskDuetoPolicy])):
                print("Lambdas: " + str(lam[0]) + ", redundant fails: " + str(float(rows[TotalTasks][failedTask])-float(rows[TotalTasks][failedTaskDuetoPolicy])))
                extra_objects_read.loc[round(float(lambda0),4),round(float(lambda1),4)]=0

    return extra_objects_read



def NsfBsfGraph():
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

    filePath = ''.join([folderPath, '\ite1'])
    all_files = [f for f in listdir(filePath) if isfile(join(filePath, f))]
    filtered_files = Filter(all_files,'READOBJECTS')
    failed_files = Filter(all_files,'NSF_TASK_FAILED')

    lambdas = []
    failed_lambdas = []
    for file in filtered_files:
        # r1 = re.findall('\d+', file )
        lambdas.append(re.findall( r'SIMRESULT_([-+]?\d*\.\d+|\d+)_([-+]?\d*\.\d+|\d+)_.*', file))
    for file in failed_files:
        # r1 = re.findall('\d+', file )
        failed_lambdas.append(re.findall( r'SIMRESULT_([-+]?\d*\.\d+|\d+)_([-+]?\d*\.\d+|\d+)_.*', file))
    extra_objects_read = get_extra_object_reads(lambdas,failed_lambdas,filtered_files,folderPath)
    read_by_type(lambdas,filtered_files,folderPath)
    # extra_objects_read = failed_tasks(lambdas,Filter(all_files,'GENERIC'),folderPath,extra_objects_read)
    g=sns.heatmap(extra_objects_read,cbar_kws={'label': 'Parities Read'},cmap=sns.cm.rocket_r)

    g.set(xlabel='λ_a', ylabel='λ_b')
    g.set_title("Service Cost")
    plt.savefig(folderPath + '\\fig\\Service Cost' + '.png', bbox_inches='tight')
    plt.show()





NsfBsfGraph()

