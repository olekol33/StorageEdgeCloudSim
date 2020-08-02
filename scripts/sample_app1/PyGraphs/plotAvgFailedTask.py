from plotGenericResult import *
def plotAvgFailedTask():
    plotGenericResult(TotalTasks, failedTask, 'Failed Tasks (%)', 'ALL_APPS', 'percentage_for_all')
    plotGenericResult(TotalTasks, failedTask, 'Failed Tasks for Augmented Reality App (%)', 'AUGMENTED_REALITY', 'percentage_for_all')
    plotGenericResult(TotalTasks, failedTask, 'Failed Tasks for Health App (%)', 'HEALTH_APP', 'percentage_for_all')
    plotGenericResult(TotalTasks, failedTask, 'Failed Tasks for Infotainment App (%)', 'INFOTAINMENT_APP', 'percentage_for_all')
    plotGenericResult(TotalTasks, failedTask, 'Failed Tasks for Heavy Comp. App (%)', 'HEAVY_COMP_APP', 'percentage_for_all')

    plotGenericResult(EdgeTasks, failedTask, 'Failed Tasks on Edge (%)', 'ALL_APPS', 'percentage_for_all')
    plotGenericResult(EdgeTasks, failedTask, 'Failed Tasks on Edge for Augmented Reality App (%)', 'AUGMENTED_REALITY', 'percentage_for_all')
    plotGenericResult(EdgeTasks, failedTask, 'Failed Tasks on Edge for Health App (%)', 'HEALTH_APP', 'percentage_for_all')
    plotGenericResult(EdgeTasks, failedTask, 'Failed Tasks on Edge for Infotainment App (%)', 'INFOTAINMENT_APP', 'percentage_for_all')
    plotGenericResult(EdgeTasks, failedTask, 'Failed Tasks on Edge for Heavy Comp. App (%)', 'HEAVY_COMP_APP', 'percentage_for_all')

    plotGenericResult(CloudTasks, failedTask, 'Failed Tasks on Cloud (%)', 'ALL_APPS', 'percentage_for_all')
    plotGenericResult(CloudTasks, failedTask, 'Failed Tasks on Cloud for Augmented Reality App (%)', 'AUGMENTED_REALITY', 'percentage_for_all')
    plotGenericResult(CloudTasks, failedTask, 'Failed Tasks on Cloud for Health App (%)', 'HEALTH_APP', 'percentage_for_all')
    plotGenericResult(CloudTasks, failedTask, 'Failed Tasks on Cloud for Infotainment App (%)', 'INFOTAINMENT_APP', 'percentage_for_all')
    plotGenericResult(CloudTasks, failedTask, 'Failed Tasks on Cloud for Heavy Comp. App (%)', 'HEAVY_COMP_APP', 'percentage_for_all')

plotAvgFailedTask()