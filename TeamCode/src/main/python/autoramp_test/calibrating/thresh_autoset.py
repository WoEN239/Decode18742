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

hueStep = 1
satStep = 50
valStep = 25
morphStep = 1

sosat = SoSAT()
goodResults = []

os.chdir(pathToTestImages)
imagesNames = os.listdir(".")

for hGreenMin in range(50,51,hueStep):
    for hGreenMax in range(90,91,hueStep):
        for sGreenMin in range(0,1,satStep):
            for sGreenMax in range(254,255,satStep):
                for vGreenMin in range(0,250,valStep):
                    for vGreenMax in range(1,250,valStep):
                        for hPurpleMin in range(120,121,hueStep):
                            for hPurpleMax in range(150,151,hueStep):
                                for sPurpleMin in range(0,1,satStep):
                                    for sPurpleMax in range(254,255,satStep):
                                        for vPurpleMin in range(0,250,valStep):
                                            for vPurpleMax in range(1,250,valStep):
                                                for openVal in range(0,4,morphStep):
                                                    for erodeVal in range(0,4,morphStep):

                                                        if hGreenMin >= hGreenMax or sGreenMin >= sGreenMax or vGreenMin >= vGreenMax or hPurpleMin >= hPurpleMax or sPurpleMin >= sPurpleMax or vPurpleMin >= vPurpleMax:
                                                           continue
                                                        
                                                        sosat.greenThreshLower = np.array([hGreenMin, sGreenMin, vGreenMin])
                                                        sosat.greenThreshUpper = np.array([hGreenMax, sGreenMax, vGreenMax])
                                                        sosat.purpleThreshLower = np.array([hPurpleMin, sPurpleMin, vPurpleMin])
                                                        sosat.purpleThreshUpper = np.array([hPurpleMax, sPurpleMax, vPurpleMax])
                                                        sosat.morphOpenVal = openVal
                                                        sosat.morphErodeVal = erodeVal

                                                        print(f"Tried: GREEN:[{hGreenMin},{sGreenMin},{vGreenMin}] to [{hGreenMax},{sGreenMax},{vGreenMax}]\nPURPLE: [{hPurpleMin},{sPurpleMin},{vPurpleMin}] to [{hPurpleMax},{sPurpleMax},{vPurpleMax}]\nOPEN Value: {openVal}, ERODE Value: {erodeVal}")

                                                        isMatch = True

                                                        for imageName in imagesNames:
                                                            frame = cv2.imread(imageName)
                                                            artefactsDetected = sosat.detectArtefactsGPU(frame,1,750,10,7)

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
                                                                "erodeVal": erodeVal
                                                            })
                                                        else:
                                                            print(f"failed\n")
                                                            
    print(f"GOOD RESULTS: {len(goodResults)}")                                                      
for result in goodResults:
    print(result)

cv2.destroyAllWindows()