import matplotlib.pyplot as plt
from matplotlib.dates import HourLocator, DateFormatter
from scipy.stats import t
import itertools
import pandas as pd
import seaborn as sns
import configparser
from OrbitPackages import *

#Parses READOBJECTS and LOST_TASKS
def parseClientTaskFiles(filePath,runDF):
    # folderPath = getConfiguration("folderPath")
    filename = os.path.basename(filePath)
    orchestratorPolicy, objectPlacement, mobileDeviceNumber = parsePolicies(filename)
    # filePath = ''.join([folderPath, '\ite1\\',filename])
    df = pd.read_csv(filePath,delimiter=';',engine='python')
    runDF = runDF.append({    'policy':objectPlacement,
        'total tasks':df.shape[0],
        'data tasks':df[df["type"]=="data"].shape[0],
        'parity tasks':df[df["type"]=="parity"].shape[0],
        'total latency':df["latency"].mean(),
        'data latency':df[df["type"]=="data"]["latency"].mean(),
        'parity latency':df[df["type"]=="parity"]["latency"].mean()},ignore_index=True)

    #Group by type
    objectCount = df.groupby(['ObjectID', 'type'], as_index=False)['ioID'].count()
    #Sort by object id
    objectCount['ObjectIDStripped'] = objectCount['ObjectID'].map(lambda x: x.lstrip('d'))
    objectCount['ObjectIDStripped'] = objectCount['ObjectIDStripped'].astype(int)
    objectCount = objectCount.sort_values('ObjectIDStripped')
    #split data and parity counts
    objectCount.rename(columns={"ioID": "dataCount"},inplace=True)
    objectCount['parityCount'] = objectCount['dataCount']
    objectCount.loc[objectCount['type']=='data','parityCount']=0
    objectCount.loc[objectCount['type']=='parity','dataCount']=0

    objectCountData = objectCount[objectCount['type']=='data'].drop(['type','parityCount','ObjectID'], axis=1)
    objectCountParity = objectCount[objectCount['type']=='parity'].drop(['type','dataCount','ObjectID'], axis=1)
    result = pd.merge(objectCountData, objectCountParity, on=['ObjectIDStripped'], how='outer')
    result['parityCount'] = result['parityCount'].fillna(0)
    result['parityCount'] = result['parityCount'].astype(int)
    result.rename(columns={"parityCount": "Parity","dataCount" : "Data"}, inplace=True)
    result["Lost"] = 0
    result = result.set_index('ObjectIDStripped')

    result = result.sort_index()

    # Lost tasks
    folderPath = os.path.dirname(filePath)
    taskFiles = getLogPathByName(folderPath,"DEVICES_LOST_TASKS")
    lostTasksFiles = Filter(taskFiles, orchestratorPolicy)
    # filePath = ''.join([folderPath, "\\",lostTasksFiles[0]])
    lostTasksDF = pd.read_csv(lostTasksFiles[0], delimiter=';')
    lostTasksDF['objectID'] = lostTasksDF['objectID'].map(lambda x: x.lstrip('object:d'))
    for object in lostTasksDF['objectID'].unique():
        numObjects = lostTasksDF[lostTasksDF['objectID']==object].shape[0]
        result.loc[int(object),"Lost"] = numObjects

    result = result.fillna(0)
    fig, ax = plt.subplots(1, 1, figsize=(12, 10))
    result.plot(kind="bar", stacked=True, ax=ax)
    fig.tight_layout(h_pad=2)
    ax.set_xlabel('Object ID')
    fig.suptitle('Read objects by operation type - ' + orchestratorPolicy + "; " + objectPlacement + "; " +
                 str(mobileDeviceNumber) + 'Devices', y=1.01)
    fig.savefig(getConfiguration("figPath")+'\\Read objects by operation type '+objectPlacement + '.png',bbox_inches='tight')
    plt.close(fig)


    return runDF

def plot3StackedGraph(rundir,runDF, name, ylabel):
    folderPath = getConfiguration("folderPath")
    #Total num of tasks
    taskFiles1 = getLogPathByName(rundir,"LOST_TASKS")
    taskFiles2 = getLogPathByName(rundir,"DEVICES_READOBJECTS")
    # filePath = ''.join([folderPath, '\ite1\\', taskFiles[0]])
    filePath1 = taskFiles1[0]
    data1 = pd.read_csv(filePath1, delimiter=',')
    filePath2 = taskFiles2[0]
    data2 = pd.read_csv(filePath2, delimiter=',')
    numOfTasks = data1.shape[0]+data2.shape[0]

    #Warm up
    # config = getLogPathByName(rundir,"default_config.properties")
    # configParser = configparser.RawConfigParser()
    # # configParser.read(folderPath+'\ite1\\'+config[0])
    # configParser.read(config[0])
    # warm_up_period = float(configParser.get('run-settings', 'warm_up_period'))*60
    # data = data[data["startTime"] > warm_up_period]



    fig,ax = plt.subplots(3, 1, figsize=(12, 10))

    for i in range(3):
        runDF.iloc[:,i].to_frame().transpose().plot.bar(ax=ax[i],use_index=True)
    if("Read" in name):
        ax[0].scatter(x=0,y=numOfTasks)
    for axis in ax:
        axis.set_ylabel(ylabel)
        axis.xaxis.set_visible(False)
        axis.grid(True)
    ax[0].set_title("All Reads")
    ax[1].set_title("Data Reads")
    ax[2].set_title("Parity Reads")
    fig.tight_layout(h_pad=2)
    fig.savefig(getConfiguration("figPath")+'\\'+name + '.png',bbox_inches='tight')
    plt.close(fig)


def plotClientTasks(filePath):
    # folderPath = getConfiguration("folderPath")
    filename = os.path.basename(filePath)
    orchestratorPolicy, objectPlacement, mobileDeviceNumber = parsePolicies(filename)
    # filePath = ''.join([folderPath, '\ite1\\',filename])
    figPath = ''.join([getConfiguration("figPath"), '\\'+objectPlacement+'\\'])
    ensure_dir(figPath)
    patterns = re.findall(r'.*(CLIENT\d).*', filename)
    clientID=patterns[0]
    df = pd.read_csv(filePath,delimiter=';',engine='python')
    fig, ax = plt.subplots(1, 1, figsize=(25, 10))
    sns.scatterplot(x='ioID',y="latency", data=df.reset_index(), hue="type",ax=ax)
    fig.tight_layout(h_pad=2)
    ax.set_xlabel('Task')
    ax.set_ylabel('Latency[sec]')
    fig.suptitle(clientID + ' Time To Complete Task - ' + orchestratorPolicy + "; " + objectPlacement + "; " +
                 str(mobileDeviceNumber) + 'Devices', y=1.01)
    fig.savefig(figPath + '\\'+clientID + ' Time To Complete Task - ' + objectPlacement + '.png',
                bbox_inches='tight')
    plt.close(fig)

    # Lost tasks
    folderPath = os.path.dirname(filePath)
    taskFiles = getLogPathByName(folderPath,"DEVICES_LOST_TASKS")
    lostTasksFiles = Filter(taskFiles, orchestratorPolicy)
    # filePath = ''.join([folderPath, '\ite1\\', lostTasksFiles[0]])
    filePath = lostTasksFiles[0]
    data = pd.read_csv(filePath, delimiter=';')
    if data.empty:
        return

    fig, ax = plt.subplots(1, 1, figsize=(15, 8))
    data['Count'] = 1
    data['time'] = data['time'].astype('timedelta64[s]')
    num_of_req = data.resample('S', on='time').Count.sum().to_frame().reset_index()
    num_of_req['time'] = num_of_req['time'].astype('timedelta64[s]')

    sns.lineplot(x='time', y="Count", data=num_of_req, ax=ax)
    ax.set_xlabel("Time(sec)")
    ax.set_ylabel("Tasks")
    fig.suptitle('Lost tasks by request time - ' + orchestratorPolicy + "; " + objectPlacement + "; " +
                 str(mobileDeviceNumber) + 'Devices', y=1.0002)
    # fig.tight_layout()
    # plt.legend()
    fig.savefig(figPath + 'Lost tasks by request time ' + orchestratorPolicy + "_" + objectPlacement + "_" +
                str(mobileDeviceNumber) + ' Devices.png', bbox_inches='tight')
    plt.close(fig)



def plotOrbitClient(rundir):
    print("Running " + plotOrbitClient.__name__)
    runDF = pd.DataFrame(columns=["policy","total tasks","data tasks","parity tasks","total latency", "data latency","parity latency"])
    taskFiles = getLogPathByName(rundir,"DEVICES_READOBJECTS")
    for file in taskFiles:
        runDF = parseClientTaskFiles(file,runDF)

    runDF = runDF.set_index("policy")
    plot3StackedGraph(rundir,runDF.iloc[:,[0,1,2]],"Read by type","Reads")
    plot3StackedGraph(rundir,runDF.iloc[:,[3,4,5]],"Latency by type","Latency[s]")

    clientTaskFiles = getLogPathByName(rundir,"_READOBJECTS")
    #Keep only client files
    clientTaskFiles = [x for x in clientTaskFiles if x not in taskFiles]
    for file in clientTaskFiles:
        plotClientTasks(file)




if __name__=="__main__":
    plotOrbitClient()