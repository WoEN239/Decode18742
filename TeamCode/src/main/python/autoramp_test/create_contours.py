import cv2
import numpy as np
from artefacts_detection import ArtefactsDetection
detector = ArtefactsDetection(contours_path="contours_data.json")

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
        end_coords[0] = x
        end_coords[1] = y  

cv2.namedWindow("Set contour",cv2.WINDOW_NORMAL)
cv2.setMouseCallback("Set contour",mouse_callback)

frame = cv2.imread("test1.jpg")
display_frame = None

while True:
    if is_selecting:
        display_frame = selecting_frame
    #elif is_selected:
    #    display_frame = frame[start_coords[1]:end_coords[1],start_coords[0]:end_coords[0]]
    
    else:
        display_frame = frame.copy()
    cv2.imshow("Set contour",display_frame)

    key = cv2.waitKey(1) & 0xFF
    if key == ord('q'):
        break