import cv2
import numpy as np
import json
import sys
import os

from sosat import SoSAT

sosat = SoSAT("contoursData.json")

isSelecting = False
isSelected = False
startCoords = [None, None]
endCoords = [None, None]

frame = None
selectingFrame = None

def mouse_callback(event,x,y,flags,param):
    global isSelecting, isSelected, startCoords, endCoords, frame, selectingFrame
    if event == cv2.EVENT_LBUTTONDOWN:
        isSelecting = True
        selectingFrame = frame.copy()
        startCoords[0] = x
        startCoords[1] = y
        
    elif event == cv2.EVENT_MOUSEMOVE and isSelecting:
        selectingFrame = frame.copy()
        cv2.rectangle(selectingFrame,(startCoords[0],startCoords[1]),(x,y),(0,255,0),2)

    elif event == cv2.EVENT_LBUTTONUP and isSelecting:
        isSelecting = False
        isSelected = True

        if(startCoords[0]>x):
            endCoords[0]=startCoords[0]
            startCoords[0]=x

        else:
            endCoords[0]=x
        
        if(startCoords[1]>y):
            endCoords[1]=startCoords[1]
            startCoords[1]=y

        else:
            endCoords[1]=y


def touches_edge(contour, width, height):
    for point in contour:
        x, y = point[0]
        if x == 0 or x == width - 1 or y == 0 or y == height - 1:
            return True
    return False

cv2.namedWindow("Set contour",cv2.WINDOW_NORMAL)

cv2.setMouseCallback("Set contour",mouse_callback)


imageNames = os.listdir("testImages/")
displayFrame = None
contoursList = []
exiting = False

for imageName in imageNames:
    
    frame = cv2.imread(f"testImages/{imageName}")
    while True:
        if isSelecting:
            displayFrame = selectingFrame

        elif isSelected and startCoords[0] is not None and endCoords[0] is not None:
            displayFrame = frame[startCoords[1]:endCoords[1],startCoords[0]:endCoords[0]]
            height, width = displayFrame.shape[:2]
            masks = sosat.colorMasks(displayFrame)

            all_contours = []
            for mask in masks:
                contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
                for contour in contours:
                    area = cv2.contourArea(contour)
                    if area > 500 and not touches_edge(contour, width, height):
                        all_contours.append((area, contour))

            all_contours.sort(key=lambda x: x[0], reverse=True)
            top_contours = [contour for _, contour in all_contours[:10]]

            for contour in top_contours:
                contoursList.append(contour.tolist())

            print(f"CONTOURS ADDED: {len(contoursList)}")
            isSelected = False

        else:
            displayFrame = frame.copy()

        cv2.imshow("Set contour",displayFrame)

        key = cv2.waitKey(1) & 0xFF
        if key == ord('s'):
            with open("contoursData.json","w") as f:
                json.dump(contoursList,f,indent=4)
                f.close()
            print(f"TOTAL CONTOURS SAVED: {len(contoursList)}")
        elif key == ord('n'):
            break
        elif key == ord('q'):
            exiting=True
            break
    if exiting:
        break