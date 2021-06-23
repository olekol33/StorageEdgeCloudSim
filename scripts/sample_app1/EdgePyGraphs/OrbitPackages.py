from getConfiguration import *
from glob import glob

def ensure_dir(file_path):
    directory = os.path.dirname(file_path)
    if not os.path.exists(directory):
        os.makedirs(directory)


#Returns host ID for file name
def parseHostID(filename):
    patterns = re.findall(r'.*HOST(\d+)_.*', filename)
    # patterns = re.findall(r'.*DEVICES_.*(\d+)_.*', filename)
    return patterns[0]

def parsePolicies(filename):
    patterns = re.findall(r'.*SINGLE_TIER_(.*)_(\d+)DEVICES.*', filename)
    substring = patterns[0]
    objectPlacements = getConfiguration("listOfObjectPlacements")
    orchestratorPolicies = getConfiguration("listOfOrchestratorPolicies")
    for exp in objectPlacements:
        if exp in substring[0]:
            placement=exp
            break
    for exp in orchestratorPolicies:
        if exp in substring[0]:
            policy=exp
            break

    return policy,placement,substring[1]


#Returns list of files with host logs
def getLogPathByName(filePath, suffix):
    files = []
    all_files = [f for f in listdir(filePath) if isfile(join(filePath, f))]
    for file in all_files:
        if suffix in file:
            files.append(filePath+"\\"+file)
    return files
    # return Filter(all_files,suffix)

def getLogsByNameFromFilepath(filePath,suffix):
    all_files = [f for f in listdir(filePath) if isfile(join(filePath, f))]
    return Filter(all_files,suffix)

def getRunlogsDirs():
    folderPath = getConfiguration("folderPath")
    return glob(''.join([folderPath, '\ite1\*\\']))

