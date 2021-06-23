from plotGenericResult import *
def plotAvgProcessingTime():
    print("Running " + plotAvgProcessingTime.__name__)
    plotGenericResult(TotalTasks, processingTime, 'Average Processing Time (sec)', 'ALL_APPS', '')
    plotGenericResult(TotalTasks, processingTime, 'Average Processing Time for Augmented Reality App (sec)', 'AUGMENTED_REALITY', '')
    plotGenericResult(TotalTasks, processingTime, 'Average Processing Time for Health App (sec)', 'HEALTH_APP', '')
    plotGenericResult(TotalTasks, processingTime, 'Average Processing Time for Infotainment App (sec)', 'INFOTAINMENT_APP', '')
    plotGenericResult(TotalTasks, processingTime, 'Average Processing Time for Heavy Comp. App (sec)', 'HEAVY_COMP_APP', '')

    plotGenericResult(EdgeTasks, processingTime, 'Average Processing Time on Edge (sec)', 'ALL_APPS', '')
    plotGenericResult(EdgeTasks, processingTime, 'Average Processing Time on Edge for Augmented Reality App (sec)', 'AUGMENTED_REALITY', '')
    plotGenericResult(EdgeTasks, processingTime, 'Average Processing Time on Edge for Health App (sec)', 'HEALTH_APP', '')
    plotGenericResult(EdgeTasks, processingTime, 'Average Processing Time on Edge for Infotainment App (sec)', 'INFOTAINMENT_APP', '')
    plotGenericResult(EdgeTasks, processingTime, 'Average Processing Time on Edge for Heavy Computation App (sec)', 'HEAVY_COMP_APP', '')

    plotGenericResult(CloudTasks, processingTime, 'Average Processing Time on Cloud (sec)', 'ALL_APPS', '')
    plotGenericResult(CloudTasks, processingTime, 'Average Processing Time on Cloud for Augmented Reality App (sec)', 'AUGMENTED_REALITY', '')
    plotGenericResult(CloudTasks, processingTime, 'Average Processing Time on Cloud for Health App (sec)', 'HEALTH_APP', '')
    plotGenericResult(CloudTasks, processingTime, 'Average Processing Time on Cloud for Infotainment App (sec)', 'INFOTAINMENT_APP', '')
    plotGenericResult(CloudTasks, processingTime, 'Average Processing Time on Cloud for Heavy Computation App (sec)', 'HEAVY_COMP_APP', '')

