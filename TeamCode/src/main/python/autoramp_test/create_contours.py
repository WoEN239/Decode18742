import cv2
import numpy as np
from artefacts_detection import ArtefactsDetection
import json

detector = ArtefactsDetection()

is_selecting = False
is_selected = False
start_coords = [None, None]
end_coords = [None, None]

frame = None
selecting_frame = None

def mouse_callback(event,x,y,flags,param):
    global is_selecting, is_selected, start_coords, end_coords, frame, selecting_frame
    if event == cv2.EVENT_LBUTTONDOWN:
        is_selecting = True
        selecting_frame = frame.copy()
        start_coords[0] = x
        start_coords[1] = y
    elif event == cv2.EVENT_MOUSEMOVE and is_selecting:
        selecting_frame = frame.copy()
        cv2.rectangle(selecting_frame,(start_coords[0],start_coords[1]),(x,y),(0,255,0),2)

    elif event == cv2.EVENT_LBUTTONUP and is_selecting:
        is_selecting = False
        is_selected = True

        if(start_coords[0]>x):
            end_coords[0]=start_coords[0]
            start_coords[0]=x
        else:
            end_coords[0]=x
        
        if(start_coords[1]>y):
            end_coords[1]=start_coords[1]
            start_coords[1]=y
        else:
            end_coords[1]=y

cv2.namedWindow("Set contour",cv2.WINDOW_NORMAL)
cv2.setMouseCallback("Set contour",mouse_callback)

frame = cv2.imread("test.jpg")
display_frame = None

contours_list = []

while True:
    if is_selecting:
        display_frame = selecting_frame
    elif is_selected and start_coords[0] is not None and end_coords[0] is not None:
        display_frame = frame[start_coords[1]:end_coords[1],start_coords[0]:end_coords[0]]
        masks = detector.color_masks(display_frame,step=5,iterations=20)
        for mask in masks:
            display_frame = frame[start_coords[1]:end_coords[1],start_coords[0]:end_coords[0]]
            contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            for contour in contours:
                if cv2.contourArea(contour) > 500:
                    contours_list.append(contour.tolist())
        print(f"CONTOURS ADDED: {len(contours_list)}")
        is_selected = False
    else:
        display_frame = frame.copy()
    cv2.imshow("Set contour",display_frame)

    key = cv2.waitKey(1) & 0xFF
    if key == ord('q'):
        break

with open("contours_data.json","w") as f:
    json.dump(contours_list,f,indent=4)
    f.close()

print(f"TOTAL CONTOURS SAVED: {len(contours_list)}")