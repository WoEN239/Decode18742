import json
import cv2
import numpy as np


cv2.namedWindow("Set contour",cv2.WINDOW_NORMAL)
is_drawing = False
x1 = 0
y1 = 0
x2 = 0
y2 = 0
selection = 0
roi_selected = False
display_frame = None

def mouse_callback(event,x,y,flags,param):
    global is_drawing,x1,y1,x2,y2,roi_selected,display_frame
    if event == cv2.EVENT_LBUTTONDOWN:
        is_drawing = True
        x1 = x
        y1 = y
    elif event == cv2.EVENT_MOUSEMOVE:
        if is_drawing and display_frame is not None:
            modif_frame = display_frame.copy()
            cv2.rectangle(modif_frame,(x1,y1),(x,y),(0,255,0),3)
            cv2.imshow("Set contour",modif_frame)
    elif event == cv2.EVENT_LBUTTONUP:
        is_drawing = False
        x2 = x
        y2 = y
        roi_selected = True
    
cv2.setMouseCallback("Set contour",mouse_callback)


green_thresh_low = (60,50,130)
green_thresh_high = (90,255,255)

purple_thresh_low = (120,20,180)
purple_thresh_high = (160,255,255)

min_area = 300

i = 0
contours_list = []

while i<12:
    full_frame = cv2.imread(f"test{i+1}.jpg")
    if full_frame is None:
        print(f"Could not load test{i+1}.jpg")
        break
    
    current_frame_contours = []
    roi_selected = False
    x1, y1, x2, y2 = 0, 0, 0, 0
    display_frame = full_frame
    cv2.imshow("Set contour", full_frame)
    
    while not roi_selected:
        key = cv2.waitKey(30)
        if key == ord('q'):
            break
        elif key == ord("n"):
            i += 1
            break
    
    if key == ord('q'):
        break
    
    if key == ord("n"):
        continue
    
    if roi_selected:
        x_min, x_max = min(x1, x2), max(x1, x2)
        y_min, y_max = min(y1, y2), max(y1, y2)
        
        user_selected = full_frame[y_min:y_max, x_min:x_max]
        frame_hsv = cv2.cvtColor(user_selected,cv2.COLOR_BGR2HSV)

        skip_frame = False

        for j in range(4):
            if skip_frame:
                break
            green_thresh_low_rt = (50,20,90+j*25)
            green_thresh_high_rt = (90,255,255)
            purple_thresh_low_rt = (120,20,90+j*25)
            purple_thresh_high_rt = (160,255,255)

            for k in range(3): 
                green_mask = cv2.inRange(frame_hsv, green_thresh_low_rt, green_thresh_high_rt)
                purple_mask = cv2.inRange(frame_hsv, purple_thresh_low_rt, purple_thresh_high_rt)

                combined_mask = cv2.bitwise_or(green_mask, purple_mask)

                kernel = np.ones((4,4), np.uint8)
                combined_mask_opened = cv2.morphologyEx(combined_mask,cv2.MORPH_OPEN,kernel,iterations=3)
                combined_mask_opened_eroded = cv2.erode(combined_mask_opened,kernel,iterations=3-k)

                combined_mask_processed = combined_mask_opened_eroded

                contours,_ = cv2.findContours(combined_mask_processed, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
                
                result_frame = user_selected.copy()
                cv2.drawContours(result_frame, contours, -1, (0, 255, 0), 2)
                
                current_frame_contours.append(contours)
        print(f"  -> Accepted (total: {len(current_frame_contours)})")

        if skip_frame and key == ord("n"):
            i += 1
            continue
        elif skip_frame and key == ord("q"):
            break

        if current_frame_contours:
            contours_list.append(current_frame_contours)
            print(f"Frame {i}: Saved {len(current_frame_contours)} contour sets")
    
    if key == ord('q'):
        break

serializable_contours = []

for frame_contours in contours_list:
    frame_data = []
    for contours in frame_contours:
        contour_set = [cnt.tolist() for cnt in contours]
        frame_data.append(contour_set)
    serializable_contours.append(frame_data)

with open("contours_data.json","w") as f:
    json.dump(serializable_contours, f, indent=4)