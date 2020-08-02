from plotGenericResult import *
def plotTaskFailureReason():

    plotGenericResult(TotalTasks, failedTaskDueToVmCapacity, 'Failed Task due to VM Capacity (%)', 'ALL_APPS', 'percentage_for_failed')
    plotGenericResult(TotalTasks, failedTaskDueToVmCapacity, 'Failed Task due to VM Capacity for Augmented Reality App (%)', 'AUGMENTED_REALITY', 'for_failed')
    plotGenericResult(TotalTasks, failedTaskDueToVmCapacity, 'Failed Task due to VM Capacity for Health App (%)', 'HEALTH_APP', 'for_failed')
    plotGenericResult(TotalTasks, failedTaskDueToVmCapacity, 'Failed Task due to VM Capacity for Infotainment App (%)', 'INFOTAINMENT_APP', 'for_failed')
    plotGenericResult(TotalTasks, failedTaskDueToVmCapacity, 'Failed Task due to VM Capacity for Heavy Computation App (%)', 'HEAVY_COMP_APP', 'for_failed')

    plotGenericResult(TotalTasks, failedTaskDuetoMobility, 'Failed Task due to Mobility (%)', 'ALL_APPS', 'percentage_for_failed')
    plotGenericResult(TotalTasks, failedTaskDuetoMobility, 'Failed Task due to Mobility for Augmented Reality App (%)', 'AUGMENTED_REALITY', 'for_failed')
    plotGenericResult(TotalTasks, failedTaskDuetoMobility, 'Failed Task due to Mobility for Health App (%)', 'HEALTH_APP', 'for_failed')
    plotGenericResult(TotalTasks, failedTaskDuetoMobility, 'Failed Task due to Mobility for Infotainment App (%)', 'INFOTAINMENT_APP', 'for_failed')
    plotGenericResult(TotalTasks, failedTaskDuetoMobility, 'Failed Task due to Mobility for Heavy Computation App (%)', 'HEAVY_COMP_APP', 'for_failed')

    plotGenericResult(NetworkData, failedTaskDuetoLanBw, 'Failed Tasks due to WLAN failure (%)', 'ALL_APPS', 'percentage_for_failed')
    plotGenericResult(NetworkData, failedTaskDuetoLanBw, 'Failed Tasks due to WLAN failure for Augmented Reality App (%)', 'AUGMENTED_REALITY', 'for_failed')
    plotGenericResult(NetworkData, failedTaskDuetoLanBw, 'Failed Tasks due to WLAN failure for Health App (%)', 'HEALTH_APP', 'for_failed')
    plotGenericResult(NetworkData, failedTaskDuetoLanBw, 'Failed Tasks due to WLAN failure for Infotainment App (%)', 'INFOTAINMENT_APP', 'for_failed')
    plotGenericResult(NetworkData, failedTaskDuetoLanBw, 'Failed Tasks due to WLAN failure for Heavy Comp. App (%)', 'HEAVY_COMP_APP', 'for_failed')

    plotGenericResult(NetworkData, failedTaskDuetoWanBw, 'Failed Tasks due to WAN failure (%)', 'ALL_APPS', 'percentage_for_failed')
    plotGenericResult(NetworkData, failedTaskDuetoWanBw, 'Failed Tasks due to WAN failure for Augmented Reality App (%)', 'AUGMENTED_REALITY', 'for_failed')
    plotGenericResult(NetworkData, failedTaskDuetoWanBw, 'Failed Tasks due to WAN failure for Health App (%)', 'HEALTH_APP', 'for_failed')
    plotGenericResult(NetworkData, failedTaskDuetoWanBw, 'Failed Tasks due to WAN failure for Infotainment App (%)', 'INFOTAINMENT_APP', 'for_failed')
    plotGenericResult(NetworkData, failedTaskDuetoWanBw, 'Failed Tasks due to WAN failure for Heavy Comp. App (%)', 'HEAVY_COMP_APP', 'for_failed')

plotTaskFailureReason()