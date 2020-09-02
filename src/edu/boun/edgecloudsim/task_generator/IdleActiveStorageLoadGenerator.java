package edu.boun.edgecloudsim.task_generator;

import edu.boun.edgecloudsim.core.SimSettings;
import edu.boun.edgecloudsim.mobility.StaticRangeMobility;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.SimUtils;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.ZipfDistribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class IdleActiveStorageLoadGenerator extends LoadGeneratorModel{
    int taskTypeOfDevices[];
        static private int numOfIOTasks=0;
    public IdleActiveStorageLoadGenerator(int _numberOfMobileDevices, double _simulationTime, String _simScenario) {
        super(_numberOfMobileDevices, _simulationTime, _simScenario);
    }

    @Override
    public void initializeModel() {
        int ioTaskID=0;
        taskList = new ArrayList<TaskProperty>();

        //exponential number generator for file input size, file output size and task length
        ExponentialDistribution[][] expRngList = new ExponentialDistribution[SimSettings.getInstance().getTaskLookUpTable().length][ACTIVE_PERIOD];

        //create random number generator for each place
        for(int i=0; i<SimSettings.getInstance().getTaskLookUpTable().length; i++) {
            if(SimSettings.getInstance().getTaskLookUpTable()[i][USAGE_PERCENTAGE] ==0)
                continue;

            expRngList[i][LIST_DATA_UPLOAD] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][DATA_UPLOAD]);
            expRngList[i][LIST_DATA_DOWNLOAD] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][DATA_DOWNLOAD]);
            expRngList[i][LIST_TASK_LENGTH] = new ExponentialDistribution(SimSettings.getInstance().getTaskLookUpTable()[i][TASK_LENGTH]);
        }

        //Each mobile device utilizes an app type (task type)
        taskTypeOfDevices = new int[numberOfMobileDevices];
        for(int i=0; i<numberOfMobileDevices; i++) {
            int randomTaskType = -1;
            double taskTypeSelector = SimUtils.getRandomDoubleNumber(0,100);
            double taskTypePercentage = 0;
            //TODO: Problematic, always starts with 0 when selecting tasks
            for (int j=0; j<SimSettings.getInstance().getTaskLookUpTable().length; j++) {
                taskTypePercentage += SimSettings.getInstance().getTaskLookUpTable()[j][USAGE_PERCENTAGE];
                // Oleg: Select task if accumulated taskTypePercentage is more than random taskTypeSelector
                if(taskTypeSelector <= taskTypePercentage){
                    randomTaskType = j;
                    break;
                }
            }
            if(randomTaskType == -1){
                SimLogger.printLine("Impossible is occured! no random task type!");
                continue;
            }

            taskTypeOfDevices[i] = randomTaskType;

            double poissonMean = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][POISSON_INTERARRIVAL];
            double activePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][ACTIVE_PERIOD];
            double idlePeriod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][IDLE_PERIOD];
            double activePeriodStartTime = SimUtils.getRandomDoubleNumber(
                    SimSettings.CLIENT_ACTIVITY_START_TIME,
                    SimSettings.CLIENT_ACTIVITY_START_TIME + activePeriod);  //active period starts shortly after the simulation started (e.g. 10 seconds)
            double virtualTime = activePeriodStartTime;
            //storage
//            double samplingMethod = SimSettings.getInstance().getTaskLookUpTable()[randomTaskType][SAMPLING_METHOD];

            ExponentialDistribution rng = new ExponentialDistribution(poissonMean);
            while(virtualTime < simulationTime) {
                double interval = rng.sample();

                if(interval <= 0){
                    SimLogger.printLine("Impossible is occured! interval is " + interval + " for device " + i + " time " + virtualTime);
                    continue;
                }
                //SimLogger.printLine(virtualTime + " -> " + interval + " for device " + i + " time ");
                virtualTime += interval;

                if(virtualTime > activePeriodStartTime + activePeriod){
                    activePeriodStartTime = activePeriodStartTime + activePeriod + idlePeriod;
                    virtualTime = activePeriodStartTime;
                    continue;
                }
                String stripeDist = SimSettings.getInstance().getStripeDist();
                String stripeID = null;
                if (stripeDist.equals("RANDOM"))
                {
                    stripeID = RedisListHandler.getRandomStripeListForDevice(1).get(0);
                }
                else if (stripeDist.equals("ZIPF"))
                {
                    stripeID = RedisListHandler.getZipfStripeListForDevice(1).get(0);
                }

//                String stripeID = RedisListHandler.getRandomStripeListForDevice(1,seed).get(0);
                String[] stripeObjects = RedisListHandler.getStripeObjects(stripeID);
                List<String> dataObjects = new ArrayList<String>(Arrays.asList(stripeObjects[0].split(" ")));
                List<String> parityObjects = new ArrayList<String>(Arrays.asList(stripeObjects[1].split(" ")));
                int isParity = 0;
                for (String objectID:dataObjects){
                    taskList.add(new TaskProperty(i,randomTaskType, virtualTime, stripeID, objectID, ioTaskID, isParity,expRngList));
                    //read one object, rest is parity
                    isParity = 1;
                }
                //Only for NEAREST_WITH_PARITY
                for (String objectID:parityObjects){
                    taskList.add(new TaskProperty(i,randomTaskType, virtualTime, stripeID, objectID, ioTaskID, isParity, expRngList));
                }
                ioTaskID++;


            }
        }
        numOfIOTasks = ioTaskID;
    }

    @Override
    public int getTaskTypeOfDevice(int deviceId) {
        // TODO Auto-generated method stub
        return taskTypeOfDevices[deviceId];
    }

    public static int getNumOfIOTasks() {
        return numOfIOTasks;
    }

}