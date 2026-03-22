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

sosat = SoSAT()
goodResults = []

os.chdir(pathToTestImages)
imagesNames = os.listdir(".")

for hGreenMin in range(45,55,hueStep):
    for hGreenMax in range(85,95,hueStep):
        for sGreenMin in range(125,135,satStep):
            for sGreenMax in range(245,255,satStep):
                for vGreenMin in range(95,135,valStep):
                    for vGreenMax in range(215,255,valStep):

                        for hPurpleMin in range(115,125,hueStep):
                            for hPurpleMax in range(145,155,hueStep):
                                for sPurpleMin in range(45,55,satStep):
                                    for sPurpleMax in range(165,175,satStep):
                                        for vPurpleMin in range(50,90,valStep):
                                            for vPurpleMax in range(215,255,valStep):

                                                for openVal in range(0,4,morphStep):
                                                    for closeVal in range(0,4,morphStep):

                                                        if hGreenMin >= hGreenMax or sGreenMin >= sGreenMax or vGreenMin >= vGreenMax or hPurpleMin >= hPurpleMax or sPurpleMin >= sPurpleMax or vPurpleMin >= vPurpleMax:
                                                           continue
                                                        
                                                        sosat.greenThreshLower = np.array([hGreenMin, sGreenMin, vGreenMin])
                                                        sosat.greenThreshUpper = np.array([hGreenMax, sGreenMax, vGreenMax])
                                                        sosat.purpleThreshLower = np.array([hPurpleMin, sPurpleMin, vPurpleMin])
                                                        sosat.purpleThreshUpper = np.array([hPurpleMax, sPurpleMax, vPurpleMax])
                                                        sosat.morphOpenVal = openVal
                                                        sosat.morphCloseVal = closeVal

                                                        print(f"Tried: GREEN:[{hGreenMin},{sGreenMin},{vGreenMin}] to [{hGreenMax},{sGreenMax},{vGreenMax}]\nPURPLE: [{hPurpleMin},{sPurpleMin},{vPurpleMin}] to [{hPurpleMax},{sPurpleMax},{vPurpleMax}]\nOPEN Value: {openVal}, CLOSE Value: {closeVal}")

                                                        isMatch = True

                                                        for imageName in imagesNames:
                                                            frame = cv2.imread(imageName)
                                                            artefactsDetected = sosat.detectArtefacts(frame,1,750,25,4)

                                                            nameMatch = re.search(r'_(\d+)', imageName)
                                                            if nameMatch:
                                                                reallyInRamp = int(nameMatch.group(1))
                                                            else:
                                                                print(f"Filename {imageName} does not match expected pattern. Skipping.")
                                                                continue

                                                            if reallyInRamp != len(artefactsDetected):
                                                                isMatch = False
                                                                break
                                                        
                                                        
                                                        print(f"DETECTED ARTEFACTS: {len(artefactsDetected)}: ", end="")

                                                        if isMatch:
                                                            print(f"SUCCESS\n")
                                                            goodResults.append({
                                                                "hGreenMin": hGreenMin,
                                                                "hGreenMax": hGreenMax,
                                                                "sGreenMin": sGreenMin,
                                                                "sGreenMax": sGreenMax,
                                                                "vGreenMin": vGreenMin,
                                                                "vGreenMax": vGreenMax,
                                                                "hPurpleMin": hPurpleMin,
                                                                "hPurpleMax": hPurpleMax,
                                                                "sPurpleMin": sPurpleMin,
                                                                "sPurpleMax": sPurpleMax,
                                                                "vPurpleMin": vPurpleMin,
                                                                "vPurpleMax": vPurpleMax,
                                                                "openVal": openVal,
                                                                "erodeVal": closeVal
                                                            })
                                                        else:
                                                            print(f"failed\n")
                                                            
    print(f"GOOD RESULTS: {len(goodResults)}")                                                      
for result in goodResults:
    print(result)

cv2.destroyAllWindows()