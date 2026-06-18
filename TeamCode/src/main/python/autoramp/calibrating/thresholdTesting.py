import cv2
import numpy as np
import os

import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

from detection import DetectArtifacts

detect = DetectArtifacts("contoursData.json")

cv2.namedWindow("Test thresholds",cv2.WINDOW_NORMAL)

def nothing(x):
    pass

cv2.createTrackbar("LowerHue","Test thresholds",0,179,nothing)
cv2.createTrackbar("LowerSaturation","Test thresholds",0,255,nothing)
cv2.createTrackbar("LowerValue","Test thresholds",0,255,nothing)
cv2.createTrackbar("UpperHue","Test thresholds",179,179,nothing)
cv2.createTrackbar("UpperSaturation","Test thresholds",255,255,nothing)
cv2.createTrackbar("UpperValue","Test thresholds",255,255,nothing)
cv2.createTrackbar("MorphOpenValue","Test thresholds",0,10,nothing)
cv2.createTrackbar("ErodeValue","Test thresholds",0,10,nothing)
cv2.createTrackbar("CloseValue","Test thresholds",0,10,nothing)


imageNames = os.listdir("testImages")
imageNum = 0

detect.imageCorrection.referenceLowerThreshold = detect.blueLowerThreshold
detect.imageCorrection.referenceUpperThreshold = detect.blueUpperThreshold
detect.imageCorrection.referenceExpectedValue = detect.blueExpectedValue

while True:
    frame = cv2.imread("testImages/" + imageNames[imageNum])
    frame = cv2.cvtColor(detect.correctImage(frame),cv2.COLOR_HSV2BGR)

    LowerHue = cv2.getTrackbarPos("LowerHue","Test thresholds")
    LowerSaturation = cv2.getTrackbarPos("LowerSaturation","Test thresholds")
    LowerValue = cv2.getTrackbarPos("LowerValue","Test thresholds")
    UpperHue = cv2.getTrackbarPos("UpperHue","Test thresholds")
    UpperSaturation = cv2.getTrackbarPos("UpperSaturation","Test thresholds")
    UpperValue = cv2.getTrackbarPos("UpperValue","Test thresholds")
    morphOpenValue = cv2.getTrackbarPos("MorphOpenValue","Test thresholds")
    erodeValue = cv2.getTrackbarPos("ErodeValue","Test thresholds")
    morphСloseValue = cv2.getTrackbarPos("CloseValue","Test thresholds")

    lowerThreshold = np.array([LowerHue, LowerSaturation, LowerValue])
    upperThreshold = np.array([UpperHue, UpperSaturation, UpperValue])

    mask = detect.colorMasks(

        cv2.cvtColor(frame,cv2.COLOR_BGR2HSV),
        lowerThreshold,
        upperThreshold,

        0,
        1,

        0,
        1,

        0,
        1,

        morphOpenValue,
        0,
        1,

        erodeValue,
        0,
        1,

        morphСloseValue,
        0,
        1
    )
    if isinstance(mask, list):
        if mask:
            mask = mask[0]
        else:
            mask = np.zeros_like(frame[:, :, 0])

    maskedImage = cv2.bitwise_and(frame, frame, mask=mask)

    result = np.concatenate((maskedImage,frame),1)
    cv2.imshow("Test thresholds",result)

    key = cv2.waitKey(10) & 0xFF
    if key == ord('q'):
        
        break

    elif key == ord('n'):
        
        print(f"[{LowerHue}, {LowerSaturation}, {LowerValue}] - [{UpperHue}, {UpperSaturation}, {UpperValue}] - MorphOpen: {morphOpenValue}, Erode: {erodeValue}, Close: {morphСloseValue}")

        if len(imageNames)-1 > imageNum:

            imageNum += 1
            print("ImageName: " + imageNames[imageNum])

        else:

            print("Nothing more")    

    elif key == ord('p'):

        print(f"[{LowerHue}, {LowerSaturation}, {LowerValue}] - [{UpperHue}, {UpperSaturation}, {UpperValue}] - MorphOpen: {morphOpenValue}, Erode: {erodeValue}, Close: {morphСloseValue}")

        if imageNum > 0:

            imageNum -= 1
            print("ImageName: " + imageNames[imageNum])

        else:

            print("Nothing more")