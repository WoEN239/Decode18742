import cv2
import numpy as np

import time

import os
import sys
import re
import json

script_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(script_dir)
sys.path.append(parent_dir)
from sosat import SoSAT

pathToTestImages = "test_images/only_in_ramp/"

hueStep = 5
satStep = 10
valStep = 5
morphStep = 1

minGreenHue = [30,70]
maxGreenHue = [50,90]
minGreenSat = [120,140]
maxGreenSat = [235,255]
minGreenVal = [80,140]
maxGreenVal = [195,255]

minPurpleHue = [110,150]
maxPurpleHue = [130,170]
minPurpleSat = [40,60]
maxPurpleSat = [160,180]
minPurpleVal = [40,110]
maxPurpleVal = [195,255]

erodeRange = [0,4]
closeRange = [0,4]
openRange = [0,4]

sosat = SoSAT()

used_combinations_file = "USED_COMBINATIONS.json"
good_results_file = "GOOD_RESULTS.json"

# Load used combinations
if os.path.exists(used_combinations_file):
    with open(used_combinations_file, 'r') as f:
        used_combinations = set(tuple(combo) for combo in json.load(f))
else:
    used_combinations = set()

# Ask user if they want to reset
reset = input("Do you want to reset used combinations? (y/n): ").strip().lower()
if reset == 'y':
    used_combinations = set()
    if os.path.exists(used_combinations_file):
        os.remove(used_combinations_file)

os.chdir(pathToTestImages)
imagesNames = os.listdir(".")

if os.path.exists(good_results_file):
    with open(good_results_file, 'r') as f:
        goodResults_dict = json.load(f)
    goodResults = [goodResults_dict.get(name, []) for name in imagesNames]
else:
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
                                                                
                                                                combo = (hGreenMin, hGreenMax, sGreenMin, sGreenMax, vGreenMin, vGreenMax, hPurpleMin, hPurpleMax, sPurpleMin, sPurpleMax, vPurpleMin, vPurpleMax, erodeVal, closeVal, openVal)
                                                                if combo in used_combinations:
                                                                    continue
                                                                
                                                                sosat.greenThreshLower = np.array([hGreenMin, sGreenMin, vGreenMin])
                                                                sosat.greenThreshUpper = np.array([hGreenMax, sGreenMax, vGreenMax])
                                                                sosat.purpleThreshLower = np.array([hPurpleMin, sPurpleMin, vPurpleMin])
                                                                sosat.purpleThreshUpper = np.array([hPurpleMax, sPurpleMax, vPurpleMax])
                                                                
                                                                sosat.morphErodeVal = erodeVal
                                                                sosat.morphCloseVal = closeVal
                                                                sosat.morphOpenVal = openVal

                                                                detected_counts = []
                                                                for imageNum in range(len(imagesNames)):

                                                                    imageName = imagesNames[imageNum]
                                                                    frame = cv2.imread(imageName)

                                                                    startTime = time.time()
                                                                    artefactsDetected, masks = sosat.detectArtefactsGPU(frame,1,1000,5,4,5,2,1,4)
                                                                    endTime = time.time()
                                                                    processingTime = endTime - startTime

                                                                    detected_counts.append(len(artefactsDetected))

                                                                    nameMatch = re.search(r'_(\d+)', imageName)
                                                                    if nameMatch:
                                                                        reallyInRamp = int(nameMatch.group(1))
                                                                    else:
                                                                        print(f"Filename {imageName} does not match expected pattern. Skipping.")
                                                                        continue

                                                                    if reallyInRamp == len(artefactsDetected):
                                                                        goodResults[imageNum].append([hGreenMin, sGreenMin, vGreenMin, hGreenMax, sGreenMax, vGreenMax, hPurpleMin, sPurpleMin, vPurpleMin, hPurpleMax, sPurpleMax, vPurpleMax, openVal, erodeVal, closeVal])

                                                                used_combinations.add(combo)

                                                                print(f"Tried:\nGREEN:[{hGreenMin},{sGreenMin},{vGreenMin}] to [{hGreenMax},{sGreenMax},{vGreenMax}]\nPURPLE: [{hPurpleMin},{sPurpleMin},{vPurpleMin}] to [{hPurpleMax},{sPurpleMax},{vPurpleMax}]\nERODE Value: {erodeVal}, CLOSE Value: {closeVal}, OPEN Value: {openVal}\nProcessed in {processingTime}\nDETECTED: {detected_counts}\nGOOD RESULTS SO FAR: {[len(goodResults[imageNum]) for imageNum in range(len(imagesNames))]}\n")
                                                        
except Exception as e:
    print(f"ERROR: {e}")

finally:
    os.chdir("../..")
    
    with open(used_combinations_file, 'w') as f:
        json.dump(list(used_combinations), f, indent=4)
    
    goodResults_dict = {imagesNames[i]: goodResults[i] for i in range(len(imagesNames))}
    with open(good_results_file, 'w') as f:
        json.dump(goodResults_dict, f, indent=4)
    
    with open("GOOD_RESULTS.txt","w") as f:
        for name, results in goodResults_dict.items():
            f.write(f"{name}: {results}\n")

    print("GOOD RESULTS:")                                                      
    for name, results in goodResults_dict.items():
        print(f"{name}: {len(results)} good combinations")
        for res in results:
            print(f"  GREEN: [{res[0]},{res[1]},{res[2]}] to [{res[3]},{res[4]},{res[5]}]")
            print(f"  PURPLE: [{res[6]},{res[7]},{res[8]}] to [{res[9]},{res[10]},{res[11]}]")
            print(f"  MORPH: OPEN {res[12]}, ERODE {res[13]}, CLOSE {res[14]}")
            print()

    cv2.destroyAllWindows()