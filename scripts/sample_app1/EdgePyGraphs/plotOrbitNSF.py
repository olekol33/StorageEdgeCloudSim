import matplotlib.pyplot as plt
from matplotlib.dates import HourLocator, DateFormatter
from scipy.stats import t
import itertools
import pandas as pd
import seaborn as sns
import configparser
from glob import glob
from NsfBsfGraphs import handleNSFObjectsRead
from OrbitGraphGenerator import *
from OrbitPackages import *
import shutil

#Get lambdas for each run
def parseLambdas(folderName):
    patterns = re.findall(r'runlogs_(.*)_(.*)$', folderName)
    return (float(patterns[0][0]),float(patterns[0][1]))

def isRunValid(folderName,threshold):

    lostTasks = getLogsByNameFromFilepath(folderName,'DEVICES_LOST_TASKS')
    folderName=folderName+"\\"
    lostTasksDF = pd.read_csv(folderName+lostTasks[0],delimiter=';')
    # tasks = getLogsByNameFromFilepath(folderName,'TASK_LIST')
    # tasksDF = pd.read_csv(folderName+tasks[0],delimiter=',')
    comnpletedTasks = getLogsByNameFromFilepath(folderName,'DEVICES_READOBJECTS')
    comnpletedTasksDF = pd.read_csv(folderName+comnpletedTasks[0],delimiter=',')

    # config = getLogsByNameFromFilepath(folderName, 'default_config.properties')
    # configParser = configparser.RawConfigParser()
    # configParser.read(folderName+config[0])
    # warm_up_period = float(configParser.get('run-settings', 'warm_up_period'))*60
    # tasksDF=tasksDF[tasksDF["startTime"]>warm_up_period]
    # lostTasksDF=lostTasksDF[lostTasksDF["time"]>warm_up_period]
    failedRatio = lostTasksDF.shape[0]/(comnpletedTasksDF.shape[0]+lostTasksDF.shape[0])
    if failedRatio>threshold:
        return False
    else:
        return True

def getNumOfClients(folderName):
    files = getLogsByNameFromFilepath(folderName, 'client_list')
    filePath = folderName + "\\" + files[0]
    num_lines = sum(1 for line in open(filePath))
    return num_lines-1

def plotOrbitNSF():
    print("Running " + plotOrbitNSF.__name__)
    #clear
    clearFigPath()

    orbitRate=525 #MB/s - set manually
    folderPath = getConfiguration("folderPath")
    folderNames = [ f.path for f in os.scandir(folderPath+"\\ite1") if f.is_dir() ]
    lambdas = []
    failedLambdas = []
    for folder in folderNames:
        OrbitGraphGenerator(folder)
        lambdaTuple=parseLambdas(folder)
        lambdas.append([lambdaTuple])
        if not isRunValid(folder,0.01):
            failedLambdas.append([lambdaTuple])

    clients = getNumOfClients(folder)
    handleNSFObjectsRead(lambdas,failedLambdas,folderNames,folderPath,clients/2,orbitRate)
    # runDF = pd.DataFrame(columns=["policy","total tasks","data tasks","parity tasks","total latency", "data latency","parity latency"])
    # taskFiles = getLogsByName("DEVICES_READOBJECTS")
    # for file in taskFiles:
    #     runDF = parseClientTaskFiles(file,runDF)
    #
    # runDF = runDF.set_index("policy")
    # plot3StackedGraph(runDF.iloc[:,[0,1,2]],"Read by type","Reads")
    # plot3StackedGraph(runDF.iloc[:,[3,4,5]],"Latency by type","Latency[s]")
    #
    # clientTaskFiles = getLogsByName("_READOBJECTS")
    # #Keep only client files
    # clientTaskFiles = [x for x in clientTaskFiles if x not in taskFiles]
    # for file in clientTaskFiles:
    #     plotClientTasks(file)




if __name__=="__main__":
    plotOrbitNSF()