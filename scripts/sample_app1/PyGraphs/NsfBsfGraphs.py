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
from matplotlib import ticker
from collections import Counter
import matplotlib
matplotlib.rcParams['ps.useafm'] = True
matplotlib.rcParams['pdf.use14corefonts'] = True
#matplotlib.rcParams['text.usetex'] = True
sns.set_style({'font.family':'serif', 'font.serif':'Times New Roman'})


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

def get_objects_read(lambdas, failed_lambdas, files, folderPath):
    extra_objects_read = pd.DataFrame()
    latencies_df = pd.DataFrame()
    tasks_df = pd.DataFrame()
    latencies = pd.DataFrame(columns=["lambda0","lambda1","Latency","Tasks"])
    for lam in lambdas:
        if lam in failed_lambdas:
            lambda0 = lam[0][0]
            lambda1 = lam[0][1]
            extra_objects_read.at[lambda1, lambda0] = np.nan
            latencies_df.at[lambda1, lambda0] = np.nan
            tasks_df.at[lambda1, lambda0] = np.nan
            continue
        lambda0 = lam[0][0]
        lambda1 = lam[0][1]
        fileName = Filter(files, ''.join(['SIMRESULT_',lambda0,'_',lambda1]))
        filePath = ''.join([folderPath, '\ite1\\',fileName[0]])
        data = pd.read_csv(filePath, delimiter=';')
        latencies=latencies.append({'lambda0':lambda0, 'lambda1':lambda1, 'Latency':data.latency.mean(),
                                    "Tasks": data.latency.count()}, ignore_index=True)

        # print(lambda0,lambda1)
        data["Read By Type"]=data["type"]
        data.loc[data["Read By Type"]=="data","Read By Type"]=1
        data.loc[data["Read By Type"]=="parity","Read By Type"]=2
        extra_objects_read.at[lambda1,lambda0] = data["Read By Type"].sum()/data["Read By Type"].count()
        latencies_df.at[lambda1,lambda0]=data.latency.mean()
        tasks_df.at[lambda1,lambda0]=data.latency.count()


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

    #Latency plot

    # sns.scatterplot(x="lambda0",
    #                 y="lambda1",
    #                 size="Latency",hue="Latency",
    #                 sizes=(20, 500),
    #                 # alpha=0.5,
    #                 data=latencies)
    # # Put the legend out of the figure
    # # plt.legend(labelspacing=1.5,loc='best')
    # plt.legend(labelspacing=1.5,loc=(1.04,0))
    # # Put the legend out of the figure
    # # plt.legend(bbox_to_anchor=(1.01, 0.54),  borderaxespad=0.)
    # plt.xlabel("λ_a", size=14)
    # plt.ylabel("λ_b", size=14)
    # plt.title("Latency as a Function of Lambdas")
    # plt.tight_layout()
    # plt.xticks(rotation=90)
    # # plt.show()
    # plt.savefig(folderPath + '\\fig\\Latency' + '.png', bbox_inches='tight')
    # plt.close()

    fig,ax=plt.subplots()
    if(len(latencies_df.index.astype(float).values)>len(latencies_df.columns.astype(float).values)):
        indices = latencies_df.index.astype(float).values
    else:
        indices = latencies_df.columns.astype(float).values
    # im = ax.pcolormesh(latencies_df.index.astype(float).values, latencies_df.columns.astype(float).values
    im = ax.pcolormesh(indices, indices
                       , latencies_df.to_numpy(),cmap=sns.cm.rocket_r)
    ax.set_xlabel(r'$\lambda_a$',fontsize=22)
    ax.set_ylabel(r'$\lambda_b$',fontsize=22)
    # fig.colorbar(im, ax=ax)
    ax.tick_params(axis='both', labelsize=14)
    cb = fig.colorbar(im, ax=ax)
    cb.ax.tick_params(labelsize=14)
    ax.set_xlim(right=2.5)
    ax.set_ylim(top=2.5)
    # cb.set_clim(0.08, 0.4)
    cb.mappable.set_clim(0.08, 0.4)
    # ax.set_title("Latency as a Function of Lambdas",y=1.01)
    # plt.show()
    plt.savefig(folderPath + '\\fig\\Latency.png', bbox_inches='tight', format='png')
    # plt.savefig(folderPath + '\\fig\\Latency.eps', bbox_inches='tight', format='eps')

    #Count plot
    # sns.scatterplot(x="lambda0",
    #                 y="lambda1",
    #                 size="Tasks",hue="Tasks",
    #                 sizes=(20, 500),
    #                 # alpha=0.5,
    #                 data=latencies)
    # # Put the legend out of the figure
    # # plt.legend(labelspacing=1.5,loc='best')
    # plt.legend(labelspacing=1.5,loc=(1.04,0))
    # # Put the legend out of the figure
    # # plt.legend(bbox_to_anchor=(1.01, 0.54),  borderaxespad=0.)
    # plt.xlabel("λ_a", size=14)
    # plt.ylabel("λ_b", size=14)
    # plt.title("Number of Completed Tasks")
    # plt.tight_layout()
    # plt.xticks(rotation=90)
    # # plt.show()
    # plt.savefig(folderPath + '\\fig\\Number of Completed Tasks' + '.png', bbox_inches='tight')
    # plt.close()

    fig,ax=plt.subplots()
    # im = ax.pcolormesh(tasks_df.index.astype(float).values, tasks_df.columns.astype(float).values
    im = ax.pcolormesh(indices, indices
                       , tasks_df.to_numpy(),cmap=sns.cm.rocket_r)
    ax.set_xlabel(r'$\lambda_a$',fontsize=22)
    ax.set_ylabel(r'$\lambda_b$',fontsize=22)
    # fig.colorbar(im, ax=ax)
    ax.tick_params(axis='both', labelsize=14)
    cb = fig.colorbar(im, ax=ax)
    cb.ax.tick_params(labelsize=14)
    # ax.set_title("Number of Completed Tasks",y=1.01)
    ax.set_xlim(right=2.5)
    ax.set_ylim(top=2.5)
    # plt.show()
    plt.savefig(folderPath + '\\fig\\Number of Completed Tasks.png', bbox_inches='tight', format ='png')
    # plt.savefig(folderPath + '\\fig\\Number of Completed Tasks.eps', bbox_inches='tight', format='eps')

    return extra_objects_read

def read_by_type(lambdas,failed_lambdas,files,folderPath):
    type_read_total = pd.DataFrame(columns=["lambdas","count"])
    type_read_data = pd.DataFrame(columns=["lambdas","count"])
    type_read_parity = pd.DataFrame(columns=["lambdas","count"])
    for lam in lambdas:
        if lam in failed_lambdas:
            continue
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
    fig2.savefig(folderPath + '\\fig\\Read_By_Type.png', bbox_inches='tight', format ='png')
    # fig2.savefig(folderPath + '\\fig\\Read_By_Type.eps', bbox_inches='tight', format='eps')

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

def average_queue_size(lambdas, failed_lambdas, all_files, type , folderPath):
    files = Filter(all_files,type)
    queues = pd.DataFrame(columns=["lambda0","lambda1","Host0","Host1","Host2"])
    for lam in lambdas:
        if lam in failed_lambdas:
            continue
        lambda0 = lam[0][0]
        lambda1 = lam[0][1]
        fileName = Filter(files, ''.join(['SIMRESULT_',lambda0,'_',lambda1]))
        filePath = ''.join([folderPath, '\ite1\\',fileName[0]])
        data = pd.read_csv(filePath, delimiter=';')
        queues=queues.append({'lambda0':lambda0, 'lambda1':lambda1, 'Host0':data[data["HostID"]==0]["Requests"].mean(),
                              'Host1':data[data["HostID"]==1]["Requests"].mean(),
                              'Host2':data[data["HostID"]==2]["Requests"].mean()}, ignore_index=True)
    fig,ax = plt.subplots(3,1,figsize=(18,12))
    for ind,host in enumerate(["Host0","Host1","Host2"]):
        sns.scatterplot(x="lambda0",
                        y="lambda1",
                        size=host, hue=host,
                        # sizes=(20,500),
                        sizes=(20,500),
                        # alpha=0.5,
                        data=queues,ax=ax[ind])
        ax[ind].set_xlabel(r'$\lambda_a$', fontsize=22)
        ax[ind].set_ylabel(r'$\lambda_b$', fontsize=22)
        ax[ind].grid(True)
        # ax[ind].set_title('Average ' + type + " of " + host)
        ax[ind].legend(labelspacing=1.5)
    fig.tight_layout(h_pad=2)
    # plt.show()
    fig.savefig(folderPath + '\\fig\\Average ' + type+'.png',bbox_inches='tight', format ='png')
    # fig.savefig(folderPath + '\\fig\\Average ' + type+'.eps',bbox_inches='tight', format='eps')

    plt.close(fig)

def plot_locations(gridFilePath,folderPath):
    df = pd.read_csv(gridFilePath, delimiter=';')

    mobile_coordinates = []
    df_mobile = df[df.ItemType == 'Mobile'].copy()
    df_host = df[df.ItemType == 'Host'].copy()
    for index, row in df_mobile.iterrows():
        mobile_coordinates.append(tuple([row.xPos, row.yPos]))
    c = Counter(mobile_coordinates)
    weights = [10 * c[(xx, yy)] for xx, yy in mobile_coordinates]
    df_mobile['s'] = pd.Series(weights, index=df_mobile.index)
    fig, ax = plt.subplots(1, 1)
    colors = ['#ff7f0e', '#1f77b4']
    ax.scatter(df_mobile.xPos, df_mobile.yPos, s=df_mobile.s, c=colors[0], label="Mobile")
    ax.scatter(df_host.xPos, df_host.yPos, c=colors[1], label="Host")
    ax.legend()
    ax.set_xlabel("xPos")
    ax.set_ylabel("yPos")
    fig.suptitle("Grid Locations",y=1.01)
    if(index>10):
        fig.savefig(folderPath + '\\fig\\Grid Locations.png',bbox_inches='tight')
        # fig.savefig(folderPath + '\\fig\\Grid Locations.eps',bbox_inches='tight', format='eps')
    plt.close(fig)



def NsfBsfGraph():
    folderPath = getConfiguration("folderPath")

    filePath = ''.join([folderPath, '\ite1'])
    all_files = [f for f in listdir(filePath) if isfile(join(filePath, f))]
    unique_configs = Filter(all_files,'MAN_QUEUE')
    filtered_files = Filter(all_files,'READOBJECTS')
    failed_files = Filter(all_files,'TASK_FAILED')

    lambdas = []
    failed_lambdas = []
    for file in unique_configs:
        # r1 = re.findall('\d+', file )
        lambdas.append(re.findall( r'SIMRESULT_([-+]?\d*\.\d+|\d+)_([-+]?\d*\.\d+|\d+)_.*', file))
    for file in failed_files:
        # r1 = re.findall('\d+', file )
        failed_lambdas.append(re.findall( r'SIMRESULT_([-+]?\d*\.\d+|\d+)_([-+]?\d*\.\d+|\d+)_.*', file))
    extra_objects_read = get_objects_read(lambdas, failed_lambdas, filtered_files, folderPath)
    for lam in lambdas:
        if lam not in failed_lambdas:
            fileName = Filter(all_files, ''.join(['SIMRESULT_', lam[0][0], '_', lam[0][1]]))
            gridFileName = Filter(fileName,'GRID_LOCATION')
            # accessFilePath = Filter(fileName,'ACCESS')
            plot_locations(''.join([folderPath, '\ite1\\',gridFileName[0]]),folderPath)


    # g=sns.heatmap(extra_objects_read,cbar_kws={'label': 'Objects Read'},vmin=1,vmax=2,cmap=sns.cm.rocket_r)

    # sns.set()
    fig, ax = plt.subplots()
    # ax.pcolormesh(bounds, bounds, values)
    if(len(extra_objects_read.index.values[::-1])>len(extra_objects_read.columns.values[::-1])):
        indices0 = extra_objects_read.index.values[::-1]
        indices1 = extra_objects_read.index.values
    else:
        indices0 = extra_objects_read.columns.values
        indices1 = extra_objects_read.columns.values[::-1]
    im = ax.pcolormesh(indices0, indices1, extra_objects_read.to_numpy(),
                  vmin=1,vmax=2,cmap=sns.cm.rocket_r)
    # ax.set_xlabel('λ_a',fontsize=22)
    ax.set_xlabel(r'$\lambda_a$',fontsize=22)
    ax.set_ylabel(r'$\lambda_b$',fontsize=22)
    ax.tick_params(axis='both', labelsize=14)
    cb = fig.colorbar(im, ax=ax)
    cb.ax.tick_params(labelsize=14)
    ax.set_xlim(right=2.5)
    ax.set_ylim(top=2.5)
    # fig.suptitle("Average Number Of Objects Read Per IO Request",y=1.01)
    # ax.set_title("Average Number Of Objects Read Per IO Request",y=1.01)
    # ax.set_xticks(xs)
    # ax.set_xticklabels(xs, rotation=90)
    # ax.set_yticks(xs)
    # ax.set_yticklabels(xs, rotation=0)
    # plt.tight_layout()
    # plt.show()
    fig.savefig(folderPath + '\\fig\\Average Number Of Objects Read Per IO Request.png', bbox_inches='tight', format ='png')
    # fig.savefig(folderPath + '\\fig\\Average Number Of Objects Read Per IO Request.eps', bbox_inches='tight', format='eps')



    # g.set(xlabel='λ_a', ylabel='λ_b')
    # plt.xticks(rotation=90)
    # # g.collections[0].colorbar.ax.set_ylim(0.99, 2)
    # g.set_title("Average Number Of Objects Read Per IO Request")
    # plt.savefig(folderPath + '\\fig\\Average Number Of Objects Read Per IO Request' + '.png', bbox_inches='tight')
    # # plt.show()

    average_queue_size(lambdas, failed_lambdas, all_files,'MAN_QUEUE', folderPath)
    average_queue_size(lambdas, failed_lambdas, all_files,'HOST_QUEUE', folderPath)
    read_by_type(lambdas,failed_lambdas,filtered_files,folderPath)



NsfBsfGraph()

