#--------------------------------------------------------------
#description
# returns a value according to the given argumentssss
# based on getConfiguration.m
#--------------------------------------------------------------
import os, sys
from pathlib import Path
from os.path import isfile, join
from os import listdir
import re

def Filter(list, subs):
    filter_data = [i for i in list if subs in i]
    return filter_data

def getConfiguration(argType):
    if (argType == "folderPath"): #1
        curPath = Path(os.path.dirname(sys.argv[0]))
        return os.path.join(curPath.parent.parent.parent, 'sim_results')
    elif (argType == "numOfSimulations"): #1
        return 1 #Number of iterations
    elif (argType == "startOfMobileDeviceLoop"):  #3
        return 100 #min number of mobile device
    elif (argType == "stepOfMobileDeviceLoop"): #4
        return 200 #step size of mobile device count
    elif (argType == "endOfMobileDeviceLoop"): #5
        return 1000 #max number of mobile device
    elif (argType == "xTickLabelCoefficient"): #6
        return 1 #xTickLabelCoefficient
    elif (argType == "scenarioType"):  #7
        # return ['SINGLE_TIER','TWO_TIER','TWO_TIER_WITH_EO']
        return ['SINGLE_TIER','TWO_TIER']
    elif (argType == "legends"):    #8
        return ['1-tier','2-tier','2-tier with EO']
#    elif (argType == 9):
#        return [10 3 12 12] #position of figure
    elif (argType == "xLabel"):   #10
        return 'Number of Mobile Devices' #Common text for x axis
    elif (argType == 11):
        return 1 #return 1 if you want to save figure as pdf
    elif (argType == 12):
        return 0 #return 1 if you want to plot errors
    elif (argType == "orchestratorPolicy"):  #13
         # return ['NEAREST_WITH_PARITY']
         # return ['UNIFORM_HOST','NEAREST_HOST','IF_CONGESTED_READ_PARITY','IF_CONGESTED_READ_ONLY_PARITY',
         #         'CLOUD_OR_NEAREST_IF_CONGESTED']
         return ['NEAREST_HOST',
                 'CLOUD_OR_NEAREST_IF_CONGESTED',
                 'IF_CONGESTED_READ_PARITY']
                 # 'CLOUD_OR_NEAREST_IF_CONGESTED','SHORTEST_QUEUE']
         # return ['UNIFORM_HOST','NEAREST_HOST','IF_CONGESTED_READ_PARITY','IF_CONGESTED_READ_ONLY_PARITY']
         # return ['NEAREST_OR_PARITY']
    elif (argType == "objectPlacement"):  #14
         # return ['DATA_PARITY_PLACE','CODING_PLACE']
         return ['CODING_PLACE','REPLICATION_PLACE','DATA_PARITY_PLACE']
         # return ['NEAREST_OR_PARITY']
    elif (argType == "runType"):
        folderPath = ''.join([getConfiguration("folderPath"), '\ite1'])
        all_files = [f for f in listdir(folderPath) if isfile(join(folderPath, f))]
        filtered_files = Filter(all_files, '.tar')
        runtype = re.findall(r'ite_(.*)\.tar', filtered_files[0])
        return runtype[0]

