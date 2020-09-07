#--------------------------------------------------------------
#description
# returns a value according to the given argumentssss
# based on getConfiguration.m
#--------------------------------------------------------------
import os, sys
from pathlib import Path

def getConfiguration(argType):
    if (argType == "folderPath"): #1
        curPath = Path(os.path.dirname(sys.argv[0]))
        return os.path.join(curPath.parent.parent.parent, 'sim_results')
    elif (argType == "numOfSimulations"): #1
        return 1 #Number of iterations
    elif (argType == "startOfMobileDeviceLoop"):  #3
        return 100 #min number of mobile device
    elif (argType == "stepOfMobileDeviceLoop"): #4
        return 100 #step size of mobile device count
    elif (argType == "endOfMobileDeviceLoop"): #5
        return 1000 #max number of mobile device
    elif (argType == "xTickLabelCoefficient"): #6
        return 1 #xTickLabelCoefficient
    elif (argType == "scenarioType"):  #7
        # return ['SINGLE_TIER','TWO_TIER','TWO_TIER_WITH_EO']
        return ['SINGLE_TIER']
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
         # return ['RANDOM_HOST','NEAREST_HOST','NEAREST_WITH_PARITY','LEAST_UTIL_IN_RANGE_WITH_PARITY','IF_CONGESTED_READ_PARITY']
         return ['RANDOM_HOST','NEAREST_HOST','IF_CONGESTED_READ_PARITY','IF_CONGESTED_READ_ONLY_PARITY']
         # return ['NEAREST_OR_PARITY']
    elif (argType == "objectPlacement"):  #14
         return ['CODING_PLACE','REPLICATION_PLACE','DATA_PARITY_PLACE']
         # return ['NEAREST_OR_PARITY']
    elif (argType == 20):
        return 1 #return 1 if graph is plotted colerful

