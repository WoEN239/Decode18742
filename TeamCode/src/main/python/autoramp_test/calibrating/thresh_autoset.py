import cv2
import numpy as np

import os
import sys
import re

script_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(script_dir)
sys.path.append(parent_dir)
from sosat import SoSAT

pathToTestImages = "test_images/only_in_ramp/"


hueStep = 5
satStep = 10
valStep = 5
morphStep = 1

minGreenHue = [45,55]
maxGreenHue = [85,95]
minGreenSat = [125,135]
maxGreenSat = [245,255]
minGreenVal = [95,135]
maxGreenVal = [215,255]

minPurpleHue = [115,125]
maxPurpleHue = [145,155]
minPurpleSat = [45,55]
maxPurpleSat = [165,175]
minPurpleVal = [50,90]
maxPurpleVal = [215,255]

erodeRange = [0,4]
closeRange = [0,4]
openRange = [0,4]

sosat = SoSAT()

os.chdir(pathToTestImages)
imagesNames = os.listdir(".")
goodResults = [[] for _ in range(len(imagesNames))]

try:
    for hGreenMin in range(minGreenHue[0],minGreenHue[1],hueStep):
        for hGreenMax in range(maxGreenHue[0],maxGreenHue[1],hueStep):
            for sGreenMin in range(minGreenSat[0],minGreenSat[1],satStep):
                for sGreenMax in range(maxGreenSat[0],maxGreenSat[1],satStep):
                    for vGreenMin in range(minGreenVal[0],minGreenVal[1],valStep):
                        for vGreenMax in range(maxGreenVal[0],maxGreenVal[1],valStep):

                            for hPurpleMin in range(minPurpleHue[0],minPurpleHue[1],hueStep):
                                for hPurpleMax in range(maxPurpleHue[0],maxPurpleHue[1],hueStep):
                                    for sPurpleMin in range(minPurpleSat[0],minPurpleSat[1],satStep):
                                        for sPurpleMax in range(maxPurpleSat[0],maxPurpleSat[1],satStep):
                                            for vPurpleMin in range(minPurpleVal[0],minPurpleVal[1],valStep):
                                                for vPurpleMax in range(maxPurpleVal[0],maxPurpleVal[1],valStep):

                                                    for erodeVal in range(erodeRange[0], erodeRange[1], morphStep):
                                                        for closeVal in range(closeRange[0], closeRange[1], morphStep):
                                                            for openVal in range(openRange[0], openRange[1], morphStep):

                                                                if hGreenMin >= hGreenMax or sGreenMin >= sGreenMax or vGreenMin >= vGreenMax or hPurpleMin >= hPurpleMax or sPurpleMin >= sPurpleMax or vPurpleMin >= vPurpleMax:
                                                                    continue
                                                                
                                                                sosat.greenThreshLower = np.array([hGreenMin, sGreenMin, vGreenMin])
                                                                sosat.greenThreshUpper = np.array([hGreenMax, sGreenMax, vGreenMax])
                                                                sosat.purpleThreshLower = np.array([hPurpleMin, sPurpleMin, vPurpleMin])
                                                                sosat.purpleThreshUpper = np.array([hPurpleMax, sPurpleMax, vPurpleMax])
                                                                
                                                                sosat.morphErodeVal = erodeVal
                                                                sosat.morphCloseVal = closeVal
                                                                sosat.morphOpenVal = openVal

                                                                for imageNum in range(len(imagesNames)):

                                                                    imageName = imagesNames[imageNum]
                                                                    frame = cv2.imread(imageName)

                                                                    artefactsDetected, masks = sosat.detectArtefacts(frame,0.3,1000,5,4,5,2,1,2)

                                                                    nameMatch = re.search(r'_(\d+)', imageName)
                                                                    if nameMatch:
                                                                        reallyInRamp = int(nameMatch.group(1))
                                                                    else:
                                                                        print(f"Filename {imageName} does not match expected pattern. Skipping.")
                                                                        continue

                                                                    if reallyInRamp == len(artefactsDetected):
                                                                        goodResults[imageNum].append([hGreenMin, sGreenMin, vGreenMin, hGreenMax, sGreenMax, vGreenMax, hPurpleMin, sPurpleMin, vPurpleMin, hPurpleMax, sPurpleMax, vPurpleMax, openVal, erodeVal, closeVal])

                                                                print(f"Tried: GREEN:[{hGreenMin},{sGreenMin},{vGreenMin}] to [{hGreenMax},{sGreenMax},{vGreenMax}]\nPURPLE: [{hPurpleMin},{sPurpleMin},{vPurpleMin}] to [{hPurpleMax},{sPurpleMax},{vPurpleMax}]\nOPEN Value: {openVal}, ERODE Value: {erodeVal}, CLOSE Value: {closeVal}\nDETECTED: {[len(artefactsDetected) for imageNum in range(len(imagesNames))]}\nGOOD RESULTS SO FAR: {[len(goodResults[imageNum]) for imageNum in range(len(imagesNames))]}\n")
                                                        
except Exception as e:
    print(f"ERROR: {e}")

finally:
    os.chdir("../..")
    with open("GOOD_RESULTS.txt","w") as f:
        for result in goodResults:
            f.write(str(result)+"\n")
        f.close()

    print(f"GOOD RESULTS: ")                                                      
    for result in goodResults:
        print(result)

    cv2.destroyAllWindows()