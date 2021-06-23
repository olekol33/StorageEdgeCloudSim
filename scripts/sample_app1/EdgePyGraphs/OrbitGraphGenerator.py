from plotOrbitHostQueue import plotOrbitHostQueue
from plotOrbitHostTasks import plotOrbitHostTasks
from plotOrbitClient import plotOrbitClient
from plotOrbitObjects import plotOrbitObjects
from plotStripesRead import *
from OrbitPackages import *
import os
import glob
import shutil
from os import listdir,makedirs
from os.path import isfile, join, exists



# plotStripesRead()
def clearFigPath():
    files = glob.glob(getConfiguration("figPath") + '\\*')
    for f in files:
        if os.path.isdir(f):
            shutil.rmtree(f)
        else:
            os.remove(f)


def OrbitGraphGenerator(rundir=getConfiguration("folderPath")+'\\ite1\\'):
    # if rundir=="":
    clearFigPath()
    baseRundir = getConfiguration("folderPath")+'\\ite1\\'
    plotOrbitHostQueue(rundir)
    plotOrbitHostTasks(rundir)
    plotOrbitClient(rundir)
    # plotOrbitObjects()
    # plotStripesRead()

    if rundir!=baseRundir:
        runName = re.findall(r'.*(runlogs.*$)', rundir)
        runName = runName[0]
        figPath = getConfiguration("figPath")+'\\'
        runFigPath=figPath+runName
        if not exists(runFigPath):
            makedirs(runFigPath)
        files = glob.glob(getConfiguration("figPath") + '\\*')
        for f in files:
            if "runlogs" in f:
                continue
            else:
                shutil.move(f,runFigPath)

if __name__=="__main__":
    OrbitGraphGenerator()