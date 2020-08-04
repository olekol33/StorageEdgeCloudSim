from plotGenericResult import *
def plotAvgProcessingTime():
    print("Running " + plotAvgProcessingTime.__name__)
    plotGenericResult(TotalTasks, processingTime, 'Processing Time (sec)', 'ALL_APPS', '')
    plotGenericResult(TotalTasks, processingTime, 'Processing Time for Augmented Reality App (sec)', 'AUGMENTED_REALITY', '')
    plotGenericResult(TotalTasks, processingTime, 'Processing Time for Health App (sec)', 'HEALTH_APP', '')
    plotGenericResult(TotalTasks, processingTime, 'Processing Time for Infotainment App (sec)', 'INFOTAINMENT_APP', '')
    plotGenericResult(TotalTasks, processingTime, 'Processing Time for Heavy Comp. App (sec)', 'HEAVY_COMP_APP', '')

    plotGenericResult(EdgeTasks, processingTime, 'Processing Time on Edge (sec)', 'ALL_APPS', '')
    plotGenericResult(EdgeTasks, processingTime, 'Processing Time on Edge for Augmented Reality App (sec)', 'AUGMENTED_REALITY', '')
    plotGenericResult(EdgeTasks, processingTime, 'Processing Time on Edge for Health App (sec)', 'HEALTH_APP', '')
    plotGenericResult(EdgeTasks, processingTime, 'Processing Time on Edge for Infotainment App (sec)', 'INFOTAINMENT_APP', '')
    plotGenericResult(EdgeTasks, processingTime, 'Processing Time on Edge for Heavy Computation App (sec)', 'HEAVY_COMP_APP', '')

    plotGenericResult(CloudTasks, processingTime, 'Processing Time on Cloud (sec)', 'ALL_APPS', '')
    plotGenericResult(CloudTasks, processingTime, 'Processing Time on Cloud for Augmented Reality App (sec)', 'AUGMENTED_REALITY', '')
    plotGenericResult(CloudTasks, processingTime, 'Processing Time on Cloud for Health App (sec)', 'HEALTH_APP', '')
    plotGenericResult(CloudTasks, processingTime, 'Processing Time on Cloud for Infotainment App (sec)', 'INFOTAINMENT_APP', '')
    plotGenericResult(CloudTasks, processingTime, 'Processing Time on Cloud for Heavy Computation App (sec)', 'HEAVY_COMP_APP', '')

