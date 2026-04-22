import cv2
import numpy as np
import os

from sosat import SoSAT

cv2.namedWindow("Test thresholds",cv2.WINDOW_NORMAL)

def nothing(x):
    pass

cv2.createTrackbar("LowerHue","Test thresholds",0,179,nothing)
cv2.createTrackbar("LowerSaturation","Test thresholds",0,255,nothing)
cv2.createTrackbar("LowerValue","Test thresholds",0,255,nothing)
cv2.createTrackbar("UpperHue","Test thresholds",0,179,nothing)
cv2.createTrackbar("UpperSaturation","Test thresholds",0,255,nothing)
cv2.createTrackbar("UpperValue","Test thresholds",0,255,nothing)
cv2.createTrackbar("MorphOpenValue","Test thresholds",0,10,nothing)
cv2.createTrackbar("ErodeValue","Test thresholds",0,10,nothing)
cv2.createTrackbar("CloseValue","Test thresholds",0,10,nothing)

sosat = SoSAT("contoursData.json")
imageNames = os.listdir("testImages")
imageNum = 0

while True:
    frame = cv2.imread("testImages/" + imageNames[imageNum])

    LowerHue = cv2.getTrackbarPos("LowerHue","Test thresholds")
    LowerSaturation = cv2.getTrackbarPos("LowerSaturation","Test thresholds")
    LowerValue = cv2.getTrackbarPos("LowerValue","Test thresholds")
    UpperHue = cv2.getTrackbarPos("UpperHue","Test thresholds")
    UpperSaturation = cv2.getTrackbarPos("UpperSaturation","Test thresholds")
    UpperValue = cv2.getTrackbarPos("UpperValue","Test thresholds")
    morphOpenValue = cv2.getTrackbarPos("MorphOpenValue","Test thresholds")
    erodeValue = cv2.getTrackbarPos("ErodeValue","Test thresholds")
    morphСloseValue = cv2.getTrackbarPos("CloseValue","Test thresholds")

    sosat.greenLowerThreshold = np.array([LowerHue, LowerSaturation, LowerValue])
    sosat.greenUpperThreshold = np.array([UpperHue, UpperSaturation, UpperValue])

    sosat.purpleLowerThreshold = np.array([LowerHue, LowerSaturation, LowerValue])
    sosat.purpleUpperThreshold = np.array([UpperHue, UpperSaturation, UpperValue])

    mask = sosat.colorMasks(frame,0,1,0,1,0,1,morphOpenIterations=morphOpenValue,erodeIterations=erodeValue,morphCloseIterations=morphСloseValue)[0]
    maskedImage = cv2.bitwise_and(frame, frame, mask=mask)
    cv2.imshow("Test thresholds",maskedImage)

    key = cv2.waitKey(10) & 0xFF
    if key == ord('q'):
        
        break

    elif key == ord('n'):
        
        print(f"[{LowerHue}, {LowerSaturation}, {LowerValue}] - [{UpperHue}, {UpperSaturation}, {UpperValue}] - MorphOpen: {morphOpenValue}, Erode: {erodeValue}, Close: {morphСloseValue}")
        imageNum += 1
        print("ImageName: " + imageNames[imageNum])

    elif key == ord('p'):

        print(f"[{LowerHue}, {LowerSaturation}, {LowerValue}] - [{UpperHue}, {UpperSaturation}, {UpperValue}] - MorphOpen: {morphOpenValue}, Erode: {erodeValue}, Close: {morphСloseValue}")
        imageNum -= 1
        print("ImageName: " + imageNames[imageNum])