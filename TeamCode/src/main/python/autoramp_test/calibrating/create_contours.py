import cv2
import numpy as np
from sosat import SoSAT
import json

detector = SoSAT()

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

cv2.namedWindow("Set contour",cv2.WINDOW_NORMAL)
cv2.setMouseCallback("Set contour",mouse_callback)

frame = cv2.imread("test_actual.png")
displayFrame = None

contours_list = []

while True:
    if isSelecting:
        displayFrame = selectingFrame

    elif isSelected and startCoords[0] is not None and endCoords[0] is not None:
        display_frame = frame[startCoords[1]:endCoords[1],startCoords[0]:endCoords[0]]
        masks = detector.colorMasks(display_frame,step=5,iterations=20)

        for mask in masks:
            display_frame = frame[startCoords[1]:endCoords[1],startCoords[0]:endCoords[0]]
            contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            for contour in contours:
                if cv2.contourArea(contour) > 100:
                    contours_list.append(contour.tolist())

        print(f"CONTOURS ADDED: {len(contours_list)}")
        isSelected = False

    else:
        display_frame = frame.copy()

    cv2.imshow("Set contour",display_frame)

    key = cv2.waitKey(1) & 0xFF
    if key == ord('s'):
        with open("contours_data.json","w") as f:
            json.dump(contours_list,f,indent=4)
            f.close()
        print(f"TOTAL CONTOURS SAVED: {len(contours_list)}")
        
    if key == ord('q'):
        print("EXITTING...")
        break