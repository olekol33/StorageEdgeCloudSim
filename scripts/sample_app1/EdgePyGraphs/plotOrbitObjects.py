from getConfiguration import *
from pandas.plotting import table
import matplotlib.pyplot as plt
import pandas as pd
import seaborn as sns
from OrbitPackages import *

def getObjectLocationsByType(filepath):
    locationsFile=getLogsByNameFromFilepath(filepath,"Object_Locations")
    policy = re.findall(r'.*runlogs_(.*)$', filepath)
    policy = policy[0]
    locationsFile = locationsFile[0]
    filePath = ''.join([filepath, '\\',locationsFile])
    data = pd.read_csv(filePath, delimiter=',')
    #strip prefix
    data['object'] = data['object'].map(lambda x: x.lstrip('object:'))
    dataDF = data[data['object'].str.contains("d")]
    parityDF = data[data['object'].str.contains("p")]

    hostDataDict = {}
    hostParityDict = {}

    hostsDF = pd.DataFrame(columns=["Host","Data Objects","ParityObjects"])

    for index, object in dataDF.iterrows():
        for host in object["locations"].split():
            if host not in hostDataDict.keys():
                hostDataDict[host]=1
            else:
                hostDataDict[host]+=1
    for index, object in parityDF.iterrows():
        for host in object["locations"].split():
            if host not in hostParityDict.keys():
                hostParityDict[host]=1
            else:
                hostParityDict[host]+=1

    dataDist = pd.DataFrame(hostDataDict.items(),columns=["Host","dataObjects"])
    parityDist = pd.DataFrame(hostParityDict.items(),columns=["Host","parityObjects"])
    mergedDF = pd.merge(dataDist, parityDist, on=['Host'], how='outer')

    mergedDF=mergedDF.fillna(0)
    mergedDF['Policy']=policy
    return mergedDF

def plotDistributionByHost(df):
    figPath = getConfiguration("figPath")
    fig, ax = plt.subplots(3, 1, figsize=(15, 17), sharey=True)
    numOfHosts = max(df.Host)
    p=0
    objectPlacements = df.Policy.unique()
    for policy in objectPlacements:
        data = df[df["Policy"]==policy]
        data.plot.bar(x='Host', y='dataObjects', ax=ax[p], stacked=True, label="Data", color='green')
        data.plot.bar(x='Host', y='parityObjects', bottom=data.dataObjects,
                      ax=ax[p], stacked=True, label="Parity", color='blue')
        ax[p].set_title(objectPlacements[p])
        ax[p].legend()
        ax[p].set_xlabel("Host Number")
        ax[p].set_ylabel("Objects in Hosts")
        p+=1
    fig.suptitle('Object Distribution By Type', y=1.01)
    fig.tight_layout()
    fig.savefig(figPath + 'Object Distribution By Type', bbox_inches='tight')
    plt.close(fig)

def plotObjectDistribution():
    locationsFile = getLogPathByName("OBJECT_DISTRIBUTION")
    locationsFile = locationsFile[0]
    folderPath = getConfiguration("folderPath")
    filePath = ''.join([folderPath, '\ite1\\',locationsFile])
    #TODO fix delimiter in file
    # data = pd.read_csv(filePath, delimiter=',')
    # dataDF = data[data['object'].str.contains("d")]
    # parityDF = data[data['object'].str.contains("p")]


def plotOrbitObjects():
    print("Running " + plotOrbitObjects.__name__)
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

    # plotObjectDistribution()
    # plotObjectLocations()
    df = pd.DataFrame()
    for runPath in getRunlogsDirs():
        df = df.append(getObjectLocationsByType(runPath))
    df=df.reset_index(drop=True)
    plotDistributionByHost(df)


if __name__=="__main__":
    plotOrbitObjects()
