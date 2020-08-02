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
        return ['SINGLE_TIER','TWO_TIER','TWO_TIER_WITH_EO']
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
    elif (argType == 20):
        return 1 #return 1 if graph is plotted colerful
"""    elif (argType == 21):
    elif (argType == 22):

    elseif(argType == 21)
        ret_val=[0.55 0 0]; %color of first line
    elseif(argType == 22)
        ret_val=[0 0.15 0.6]; %color of second line
    elseif(argType == 23)
        ret_val=[0 0.23 0]; %color of third line
    elseif(argType == 24)
        ret_val=[0.6 0 0.6]; %color of fourth line
    elseif(argType == 25)
        ret_val=[0.08 0.08 0.08]; %color of fifth line
    elseif(argType == 26)
        ret_val=[0 0.8 0.8]; %color of sixth line
    elseif(argType == 27)
        ret_val=[0.8 0.4 0]; %color of seventh line
    elseif(argType == 28)
        ret_val=[0.8 0.8 0]; %color of eighth line
    elseif(argType == 40)
        ret_val={'-k*','-ko','-ks','-kv','-kp','-kd','-kx','-kh'}; %line style (marker) of the colerless line
    elseif(argType == 50)
        ret_val={':k*',':ko',':ks',':kv',':kp',':kd',':kx',':kh'}; %line style (marker) of the colerfull line
    end
end"""