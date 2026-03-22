import cv2
import numpy as np
import os
import sys
script_dir = os.path.dirname(os.path.abspath(__file__))
parent_dir = os.path.dirname(script_dir)
sys.path.append(parent_dir)
from sosat import SoSAT

def nothing(x):
    pass

sosat = SoSAT()

cv2.namedWindow("Set thresholds",cv2.WINDOW_NORMAL)

cv2.createTrackbar("H Min", "Set thresholds", 0, 255, nothing)
cv2.createTrackbar("H Max", "Set thresholds", 255, 255, nothing)
cv2.createTrackbar("S Min", "Set thresholds", 0, 255, nothing)
cv2.createTrackbar("S Max", "Set thresholds", 255, 255, nothing)
cv2.createTrackbar("V Min", "Set thresholds", 0, 255, nothing)
cv2.createTrackbar("V Max", "Set thresholds", 255, 255, nothing)
cv2.createTrackbar("Morph Open value", "Set thresholds", 0, 5, nothing)
cv2.createTrackbar("Morph Erode value", "Set thresholds", 0, 5, nothing)
cv2.createTrackbar("Morph Close value", "Set thresholds", 0, 5, nothing)



pathToTestImages = "test_images/only_in_ramp/"
os.chdir(pathToTestImages)
imageNames = os.listdir(".")

for imageName in imageNames:
    image = cv2.imread(imageName)
    hsv = cv2.cvtColor(image,cv2.COLOR_BGR2HSV)

    while True:
        hMin = cv2.getTrackbarPos("H Min", "Set thresholds")
        hMax = cv2.getTrackbarPos("H Max", "Set thresholds")
        sMin = cv2.getTrackbarPos("S Min", "Set thresholds")
        sMax = cv2.getTrackbarPos("S Max", "Set thresholds")
        vMin = cv2.getTrackbarPos("V Min", "Set thresholds")
        vMax = cv2.getTrackbarPos("V Max", "Set thresholds")
        morphOpenVal = cv2.getTrackbarPos("Morph Open value", "Set thresholds")
        morphErodeVal = cv2.getTrackbarPos("Morph Erode value", "Set thresholds")
        morphCloseVal = cv2.getTrackbarPos("Morph Close value", "Set thresholds")


        lower = np.array([hMin, sMin, vMin])
        upper = np.array([hMax, sMax, vMax])

        mask_combined = cv2.inRange(hsv, lower, upper)
        kernel = np.ones((3,3),np.uint8)
        mask_combined = cv2.morphologyEx(mask_combined, cv2.MORPH_OPEN, kernel, iterations=morphOpenVal)
        mask_combined = cv2.erode(mask_combined, kernel, iterations=morphErodeVal)
        mask_combined = cv2.morphologyEx(mask_combined, cv2.MORPH_CLOSE, kernel, iterations=morphCloseVal)

        cv2.imshow("Set thresholds", mask_combined)

        if cv2.waitKey(1) & 0xFF == ord('n'):
            print(f"{lower}, {upper}")
            break

cv2.destroyAllWindows()