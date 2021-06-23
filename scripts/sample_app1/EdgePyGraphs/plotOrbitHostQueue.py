from getConfiguration import *
from pandas.plotting import table
import matplotlib.pyplot as plt
import pandas as pd
import numpy as np
from scipy import stats
import seaborn as sns
from OrbitPackages import *

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


def plotPoolTiming(filePath):
    # folderPath = getConfiguration("folderPath")
    # filePath = ''.join([folderPath, '\ite1\\',filename])
    # fig, ax = plt.subplots(1, 1, figsize=(20, 10))
    filename = os.path.basename(filePath)
    data = pd.read_csv(filePath, delimiter=';')
    NUM_COLORS = len(data["src"].unique())
    orchestratorPolicy,objectPlacement,mobileDeviceNumber=parsePolicies(filename)
    figPath = ''.join([getConfiguration("figPath"), '\\'+objectPlacement+'\\'])
    ensure_dir(figPath)
    if "CLIENT" in filename: #skip clients for now
        return
    hostID = parseHostID(filename)
    cm = plt.get_cmap('gist_rainbow')
    delaysForHost = pd.DataFrame(columns=[orchestratorPolicy])
    hostDelaySummary = pd.DataFrame(columns=['Total','RTT','Read Delay'])

    data = data[data["Queue Size"]<250] #tmp
    # data = data[(np.abs(stats.zscore(data["Wait Time"])) < 4)]
    # data = data[(np.abs(stats.zscore(data["Queue Size"])) < 4)]

    c = 0
    fig, ax = plt.subplots(2, 1,figsize=(15,10))
    for host in data["src"].sort_values().unique():
        host_data = data[data["src"] == host].copy()
        # host_data['Wait Time'] = (host_data['Wait Time']*1000000).astype('timedelta64[us]')
        # num_of_req = host_data.resample('S', on='Wait Time').Count.avg().to_frame().reset_index()
        sns.lineplot(x="Insert Time", y="Wait Time", data=host_data, label=host, color=cm(1. * c / NUM_COLORS), ax=ax[0], ci=None)
        sns.lineplot(x="Insert Time", y="Queue Size", data=host_data, label=host, color=cm(1. * c / NUM_COLORS), ax=ax[1], ci=None)
        c += 1


    # ax.set_xlabel("Time(sec)")
    # ax.set_ylabel("Latency(sec)")
    fig.suptitle('Host' + hostID+ ' Thread Pool Wait Time - ' + orchestratorPolicy + "; " + objectPlacement + "; " +
                 str(mobileDeviceNumber) + 'Devices[sec]', y=1.01)
    ax[0].set_title("Wait Time")
    ax[1].set_title("Queue Size")
    fig.tight_layout()
    fig.savefig(figPath + 'HOST_'+hostID+' Thread Pool Wait Time ' + orchestratorPolicy + "_" + objectPlacement + "_" +
                str(mobileDeviceNumber) + ' Devices.png', bbox_inches='tight')
    plt.close(fig)

def plotHostDelay(filePath,hostLatenciesDict):

    filename=os.path.basename(filePath)
    # folderPath = getConfiguration("folderPath")
    # filePath = ''.join([folderPath, '\ite1\\',filename])
    # fig, ax = plt.subplots(1, 1, figsize=(20, 10))
    data = pd.read_csv(filePath, delimiter=';')
    data = data[(np.abs(stats.zscore(data["RTT"])) < 3)]
    data = data[(np.abs(stats.zscore(data["Read Delay"])) < 3)]
    NUM_COLORS = len(data["HostID"].unique())
    orchestratorPolicy,objectPlacement,mobileDeviceNumber=parsePolicies(filename)
    figPath = ''.join([getConfiguration("figPath"), '\\'+objectPlacement+'\\'])
    ensure_dir(figPath)
    hostID = parseHostID(filename)
    cm = plt.get_cmap('gist_rainbow')

    # hostLatencies.append(pd.Series(name=hostID))
    # hostLatencies = hostLatencies.append(pd.Series(name=hostID))

    delaysForHost = pd.DataFrame(columns=[orchestratorPolicy])
    hostDelaySummary = pd.DataFrame(columns=['Total','RTT','Read Delay'])

    c = 0

    ##Plot delay from hosts
    fig, ax = plt.subplots(3,1,figsize=(15,10),sharey=False)
    for host in data["HostID"].sort_values().unique():
        host_data = data[data["HostID"] == host]
        #Avoid incorrect values appearing at end of run
        host_data = host_data[host_data['RTT']<10]
        delaysForHost.at[host,orchestratorPolicy]=host_data[host_data['HostID']==host]['Total'].mean();
        hostDelaySummary.at[host,'Total']=host_data[host_data['HostID']==host]['Total'].mean();
        hostDelaySummary.at[host,'RTT']=host_data[host_data['HostID']==host]['RTT'].mean();
        hostDelaySummary.at[host,'Read Delay']=host_data[host_data['HostID']==host]['Read Delay'].mean();
        sns.lineplot(x="Time", y="Total", data=host_data, label=host, color=cm(1. * c / NUM_COLORS), ax=ax[0], ci=None)
        sns.lineplot(x="Time", y="RTT", data=host_data, label=host, color=cm(1. * c / NUM_COLORS), ax=ax[1], ci=None)
        sns.lineplot(x="Time", y="Read Delay", data=host_data, label=host, color=cm(1. * c / NUM_COLORS), ax=ax[2], ci=None)
        c += 1


    # ax.set_xlabel("Time(sec)")
    # ax.set_ylabel("Latency(sec)")
    fig.suptitle('Host' + hostID+ ' Delay To Other Hosts - ' + orchestratorPolicy + "; " + objectPlacement + "; " +
                 str(mobileDeviceNumber) + 'Devices[sec]', y=1.01)
    fig.tight_layout()
    fig.savefig(figPath + 'HOST_'+hostID+'_DELAY_' + orchestratorPolicy + "_" + objectPlacement + "_" +
                str(mobileDeviceNumber) + ' Devices.png', bbox_inches='tight')
    plt.close(fig)

    #Plot TCP windows
    c=0
    fig, ax = plt.subplots(2,1,figsize=(15,10),sharey=False)
    maxRcvWnd = data['rcv_wnd'].max()
    maxSndWnd = data['snd_wnd'].max()
    for host in data["HostID"].sort_values().unique():
        host_data = data[data["HostID"] == host]
        #Avoid incorrect values appearing at end of run
        host_data = host_data[host_data['RTT']<1]
        #Avoid lines on each other
        host_data['rcv_wnd'] = host_data['rcv_wnd']+maxRcvWnd*0.01*c
        host_data['snd_wnd'] = host_data['snd_wnd']+maxSndWnd*0.01*c
        sns.lineplot(x="Time", y="rcv_wnd", data=host_data, label=host, color=cm(1. * c / NUM_COLORS), ax=ax[0],ci=None)
        sns.lineplot(x="Time", y="snd_wnd", data=host_data, label=host, color=cm(1. * c / NUM_COLORS), ax=ax[1],ci=None)
        c += 1


    # ax.set_xlabel("Time(sec)")
    # ax.set_ylabel("Latency(sec)")
    fig.suptitle('Host' + hostID+ ' TCP Windows To Other Hosts - ' + orchestratorPolicy + "; " + objectPlacement + "; " +
                 str(mobileDeviceNumber) + 'Devices[sec]', y=1.01)
    fig.tight_layout()
    fig.savefig(figPath + 'HOST_'+hostID+'_TCP_' + orchestratorPolicy + "_" + objectPlacement + "_" +
                str(mobileDeviceNumber) + ' Devices.png', bbox_inches='tight')
    plt.close(fig)


    #to ms
    hostDelaySummary = hostDelaySummary*1000
    #Create table for each host
    if hostID not in hostLatenciesDict:
        hostLatencies = pd.DataFrame(columns=getConfiguration("listOfOrchestratorPolicies"))
        hostLatencies = pd.concat([hostLatencies, delaysForHost])
        hostLatenciesDict[hostID] = hostLatencies
    else:
        hostLatenciesDict[hostID] = pd.concat([hostLatenciesDict[hostID], delaysForHost])


    # #Print mean delays to png
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
    #              str(mobileDeviceNumber) + 'Devices[ms]', y=1.001)
    # fig.tight_layout()
    # plt.savefig(figPath+'HOST_'+hostID+'_MEAN_DELAY_TABLE_' + orchestratorPolicy + "_" + objectPlacement + "_" +
    #             str(mobileDeviceNumber) + ' Devices.png')

def plotOrbitHostQueue(rundir):
    print("Running " + plotOrbitHostQueue.__name__)

    hostLatenciesDict = {}

    delayFiles = getLogPathByName(rundir,"DELAYS")
    for file in delayFiles:
        plotHostDelay(file,hostLatenciesDict)

    poolFiles = getLogPathByName(rundir,"THREAD_POOL_TIMING")
    for file in poolFiles:
        plotPoolTiming(file)



if __name__=="__main__":
    plotOrbitHostQueue()
