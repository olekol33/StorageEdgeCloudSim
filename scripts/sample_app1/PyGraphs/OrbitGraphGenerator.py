from plotOrbitHostQueue import plotOrbitHostQueue
from plotOrbitHostTasks import plotOrbitHostTasks
from plotOrbitClient import plotOrbitClient
from plotStripesRead import *
from OrbitPackages import *
import os
import glob
import shutil




# plotStripesRead()



def OrbitGraphGenerator():
    files = glob.glob(getConfiguration("figPath")+'\\*')
    for f in files:
        if os.path.isdir(f):
            shutil.rmtree(f)
        else:
            os.remove(f)

    plotOrbitHostQueue()
    plotOrbitHostTasks()
    plotOrbitClient()
    # plotStripesRead()

if __name__=="__main__":
    OrbitGraphGenerator()