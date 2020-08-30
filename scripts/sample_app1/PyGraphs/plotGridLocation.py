from getConfiguration import getConfiguration
import matplotlib.pyplot as plt
import pandas as pd
from collections import Counter

# folderPath = "C:\\Users\\Oleg\\git\\EdgeCloudSim\\sim_results"
folderPath = getConfiguration("folderPath")
numOfSimulations = getConfiguration("numOfSimulations")
startOfMobileDeviceLoop = getConfiguration("startOfMobileDeviceLoop")
stepOfMobileDeviceLoop = getConfiguration("stepOfMobileDeviceLoop")
endOfMobileDeviceLoop = getConfiguration("endOfMobileDeviceLoop")
xTickLabelCoefficient = getConfiguration("xTickLabelCoefficient")
scenarioType = getConfiguration("scenarioType");
orchestratorPolicies = getConfiguration("orchestratorPolicy");

numOfMobileDevices = int((endOfMobileDeviceLoop - startOfMobileDeviceLoop) / stepOfMobileDeviceLoop + 1)

for s in range(numOfSimulations):
    for i in range(len(scenarioType)):
        for j in range(numOfMobileDevices):
            for o, orchestratorPolicy in enumerate(orchestratorPolicies):
                fields = []
                rows = []
                mobileDeviceNumber = startOfMobileDeviceLoop + stepOfMobileDeviceLoop * j
                filePath = ''.join([folderPath, '\ite', str(s + 1), '\SIMRESULT_', str(scenarioType[0]), '_', orchestratorPolicy,'_',
                                    str(mobileDeviceNumber), 'DEVICES_', 'GRID_LOCATION.log'])
                df = pd.read_csv(filePath, delimiter=';')

                mobile_coordinates = []
                df_mobile = df[df.ItemType == 'Mobile'].copy()
                df_host = df[df.ItemType == 'Host'].copy()
                for index, row in df_mobile.iterrows():
                    mobile_coordinates.append(tuple([row.xPos, row.yPos]))
                c = Counter(mobile_coordinates)
                weights = [10 * c[(xx, yy)] for xx, yy in mobile_coordinates]
                df_mobile['s'] = pd.Series(weights, index=df_mobile.index)
                fig, ax = plt.subplots(1, 1)
                colors = ['#ff7f0e', '#1f77b4']
                ax.scatter(df_mobile.xPos, df_mobile.yPos, s=df_mobile.s, c=colors[0], label="Mobile")
                ax.scatter(df_host.xPos, df_host.yPos, c=colors[1], label="Host")
                ax.legend()
                ax.set_xlabel("xPos")
                ax.set_ylabel("yPos")
                fig.suptitle("GRID_LOCATIONS_" + str(scenarioType[i]) + "_" +
                            str(mobileDeviceNumber) + "DEVICES")
                fig.savefig(folderPath + '\\fig\\' + "GRID_LOCATIONS_" + str(scenarioType[i]) + "_" +
                            orchestratorPolicy+ "_" + str(mobileDeviceNumber) + "DEVICES" + '.png', bbox_inches='tight')
                plt.close(fig)



                filePath = ''.join([folderPath, '\ite', str(s + 1), '\SIMRESULT_', str(scenarioType[0]), '_', orchestratorPolicy,'_',
                                    str(mobileDeviceNumber), 'DEVICES_', 'DEVICE_ACCESS.log'])
                df = pd.read_csv(filePath, delimiter=';')
                grouped_hosts = df.groupby(['HostID']).size()
                grouped_hosts.columns = ['HostID', 'Devices']
                fig2, ax2 = plt.subplots(1, 1)
                grouped_hosts.plot.bar("HostID","Devices", ax=ax2)
                ax2.set_xlabel("HostID")
                ax2.set_ylabel("Devices")
                fig2.suptitle("DEVICE_ACCESS_" + str(scenarioType[i]) + "_" +
                            str(mobileDeviceNumber) + "DEVICES")
                fig2.savefig(folderPath + '\\fig\\' + "GRID_LOCATIONS_" + str(scenarioType[i]) + "_" +
                            orchestratorPolicy+ "_" + str(mobileDeviceNumber) + "DEVICE_ACCESS" + '.png', bbox_inches='tight')
                plt.close(fig2)
                break
