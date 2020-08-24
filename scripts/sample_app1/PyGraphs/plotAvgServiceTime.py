from plotGenericResult import *
def plotAvgServiceTime():
    print("Running " + plotAvgServiceTime.__name__)
    plotGenericResult(TotalTasks, serviceTime, 'Average Service Time (sec)', 'ALL_APPS', '')
    plotGenericResult(TotalTasks, serviceTime, 'Average Service Time for Augmented Reality App (sec)', 'AUGMENTED_REALITY', '')
    plotGenericResult(TotalTasks, serviceTime, 'Average Service Time for Health App (sec)', 'HEALTH_APP', '')
    plotGenericResult(TotalTasks, serviceTime, 'Average Service Time for Infotainment App (sec)', 'INFOTAINMENT_APP', '')
    plotGenericResult(TotalTasks, serviceTime, 'Average Service Time for Compute Intensive App (sec)', 'HEAVY_COMP_APP', '')

    plotGenericResult(EdgeTasks, serviceTime, 'Average Service Time on Edge (sec)', 'ALL_APPS', '')
    plotGenericResult(EdgeTasks, serviceTime, 'Average Service Time on Edge for Augmented Reality App (sec)', 'AUGMENTED_REALITY', '')
    plotGenericResult(EdgeTasks, serviceTime, 'Average Service Time on Edge for Health App (sec)', 'HEALTH_APP', '')
    plotGenericResult(EdgeTasks, serviceTime, 'Average Service Time on Edge for Infotainment App (sec)', 'INFOTAINMENT_APP', '')
    plotGenericResult(EdgeTasks, serviceTime, 'Average Service Time on Edge for Heavy Comp. App (sec)', 'HEAVY_COMP_APP', '')

    plotGenericResult(CloudTasks, serviceTime, 'Average Service Time on Cloud (sec)', 'ALL_APPS', '')
    plotGenericResult(CloudTasks, serviceTime, 'Average Service Time on Cloud for Augmented Reality App (sec)', 'AUGMENTED_REALITY', '')
    plotGenericResult(CloudTasks, serviceTime, 'Average Service Time on Cloud for Health App (sec)', 'HEALTH_APP', '')
    plotGenericResult(CloudTasks, serviceTime, 'Average Service Time on Cloud for Infotainment App (sec)', 'INFOTAINMENT_APP', '')
    plotGenericResult(CloudTasks, serviceTime, 'Average Service Time on Cloud for Heavy Comp. App (sec)', 'HEAVY_COMP_APP', '')
