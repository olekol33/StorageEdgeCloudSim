from getConfiguration import *
import numpy as np
import csv
import matplotlib.pyplot as plt
from matplotlib.dates import HourLocator, DateFormatter
from scipy.stats import t
import itertools
import pandas as pd
import seaborn as sns
from OrbitPackages import *
from os import path
import scipy.stats as stats
from pandas.plotting import table


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

def plotHostTasks(filePath,hostLatenciesDict):
    filename = os.path.basename(filePath)
    data = pd.read_csv(filePath, delimiter=';')
    if data.empty:
        return
    fig, ax = plt.subplots(2, 1, figsize=(20, 10))
    NUM_COLORS = len(data["srcHost"].unique())
    orchestratorPolicy,objectPlacement,mobileDeviceNumber=parsePolicies(filename)
    figPath = ''.join([getConfiguration("figPath"), '\\'+objectPlacement+'\\'])
    ensure_dir(figPath)
    hostID = parseHostID(filename)
    cm = plt.get_cmap('gist_rainbow')

    c = 0
    for host in data["srcHost"].sort_values().unique():
        host_data = data[data["srcHost"] == host]
        #Avoid incorrect values appearing at end of run
        host_data = host_data[host_data['event']=='REQUEST']
        host_data['Time']=pd.to_timedelta(host_data['Time'], unit='S') #convert to time format
        host_data['Count']=1

        #group by seconds
        num_of_req = host_data.resample('S', on='Time').Count.sum().to_frame().reset_index()
        num_of_req['Time'] =num_of_req['Time'].astype('timedelta64[s]')

        #group by 0.1 seconds
        probMean = host_data.resample('S', on='Time').parityProb.mean().to_frame().reset_index()
        probMean['Time'] =probMean['Time'].astype('timedelta64[s]')

        sns.lineplot(x='Time',y="Count", data=num_of_req, label=host, color=cm(1. * c / NUM_COLORS), ax=ax[0])
        sns.lineplot(x='Time',y="parityProb", data=probMean, label=host, color=cm(1. * c / NUM_COLORS), ax=ax[1])

        c += 1

    ax[0].set_xlabel("Time(sec)")
    ax[0].set_ylabel("Events")
    fig.suptitle('Host' + hostID+ ' Requests To Other Hosts - ' + orchestratorPolicy + "; " + objectPlacement + "; " +
                 str(mobileDeviceNumber) + 'Devices', y=1.01)
    # fig.tight_layout()
    plt.legend()
    fig.savefig(figPath + 'HOST_'+hostID+'_HOST_REQUESTS_' + orchestratorPolicy + "_" + objectPlacement + "_" +
                str(mobileDeviceNumber) + ' Devices.png', bbox_inches='tight')
    plt.close(fig)

    #Print mean delays to png
    # # set fig size
    # fig, ax = plt.subplots(figsize=(12, 1.5))
    # # no axes
    # ax.xaxis.set_visible(False)
    # ax.yaxis.set_visible(False)
    # # no frame
    # ax.set_frame_on(False)
    # # plot table
    # tab = table(ax, hostDelaySummary, loc='upper right')
    # # set font manually
    # tab.auto_set_font_size(False)
    # tab.set_fontsize(10)
    # # save the result
    # fig.suptitle('HOST_'+hostID+'_MEAN_DELAY_TABLE_' + orchestratorPolicy + "; " + objectPlacement + "; " +
    #              str(mobileDeviceNumber) + 'Devices', y=1.001)
    # fig.tight_layout()
    # plt.savefig(figPath+'\\HOST_'+hostID+'_MEAN_DELAY_TABLE_' + orchestratorPolicy + "_" + objectPlacement + "_" +
    #             str(mobileDeviceNumber) + ' Devices.png')

    #Plot tasks processing duration
    taskSortedDF = data.sort_values(by="ioTaskID")
    taskSortedDF_req = taskSortedDF[taskSortedDF['event']=='REQUEST']
    taskSortedDF_rsp = taskSortedDF[taskSortedDF['event']=='RESPONSE']
    mergedDF = pd.merge(taskSortedDF_req, taskSortedDF_rsp, on=['ioTaskID','ObjectID','isParity','AccessID','srcHost'], how='outer')
    mergedDF['taskDuration']=mergedDF['Time_y']-mergedDF['Time_x']
    mergedDF.rename(columns={'isParity': 'Type'}, inplace=True)
    mergedDF.loc[mergedDF['Type']==1,'Type'] = 'Parity'
    mergedDF.loc[mergedDF['Type']==0,'Type'] = 'Data'

    fig, ax = plt.subplots(1, 1, figsize=(25, 10))
    sns.scatterplot(x='Time_y',y="taskDuration", data=mergedDF.reset_index(),
                    palette=sns.color_palette("gist_rainbow", mergedDF['srcHost'].nunique()),hue="srcHost",ax=ax)
    fig.tight_layout(h_pad=2)
    ax.set_xlabel('Response Time[sec]')
    ax.set_ylabel('Latency[sec]')
    fig.suptitle('Host '+hostID + ' Time To Complete Task - ' + orchestratorPolicy + "; " + objectPlacement + "; " +
                 str(mobileDeviceNumber) + 'Devices', y=1.01)
    fig.savefig(figPath + '\\Host'+hostID + ' Time To Complete Task - ' + objectPlacement + '.png',
                bbox_inches='tight')
    plt.close(fig)


def plotOrbitHostTasks(rundir):
    print("Running " + plotOrbitHostTasks.__name__)


    hostLatenciesDict = {}

    taskFiles = getLogPathByName(rundir,"HOST_TASKS")
    for file in taskFiles:
        plotHostTasks(file,hostLatenciesDict)

if __name__=="__main__":
    plotOrbitHostTasks()
