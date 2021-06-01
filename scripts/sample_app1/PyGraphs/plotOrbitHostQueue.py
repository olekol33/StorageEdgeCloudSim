from getConfiguration import *
from pandas.plotting import table
import matplotlib.pyplot as plt
import pandas as pd
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





def plotHostDelay(filename,hostLatenciesDict):


    folderPath = getConfiguration("folderPath")
    filePath = ''.join([folderPath, '\ite1\\',filename])
    # fig, ax = plt.subplots(1, 1, figsize=(20, 10))
    data = pd.read_csv(filePath, delimiter=';')
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
        sns.lineplot(x="Time", y="rcv_wnd", data=host_data, label=host, color=cm(1. * c / NUM_COLORS), ax=ax[0])
        sns.lineplot(x="Time", y="snd_wnd", data=host_data, label=host, color=cm(1. * c / NUM_COLORS), ax=ax[1])
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

def plotOrbitHostQueue():
    print("Running " + plotOrbitHostQueue.__name__)
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

    # cm = plt.get_cmap('gist_rainbow')

    hostLatenciesDict = {}

    delayFiles = getLogsByName("DELAYS")
    for file in delayFiles:
        plotHostDelay(file,hostLatenciesDict)



if __name__=="__main__":
    plotOrbitHostQueue()
