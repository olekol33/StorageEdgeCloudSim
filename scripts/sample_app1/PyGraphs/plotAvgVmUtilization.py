from plotGenericResult import *
def plotAvgVmUtilization():
    plotGenericResult(EdgeTasks, vmLoadOnClould, 'Average VM Utilization (%)', 'ALL_APPS', '')
    plotGenericResult(EdgeTasks, vmLoadOnClould, 'Average VM Utilization for Augmented Reality App (%)', 'AUGMENTED_REALITY', '')
    plotGenericResult(EdgeTasks, vmLoadOnClould, 'Average VM Utilization for Health App (%)', 'HEALTH_APP', '')
    plotGenericResult(EdgeTasks, vmLoadOnClould, 'Average VM Utilization for Infotainment App (%)', 'INFOTAINMENT_APP', '')
    plotGenericResult(EdgeTasks, vmLoadOnClould, 'Average VM Utilization for Heavy Comp. App (%)', 'HEAVY_COMP_APP', '')

plotAvgVmUtilization()