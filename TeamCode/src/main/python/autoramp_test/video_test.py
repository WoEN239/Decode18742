import cv2
import json
import numpy as np
import time


path = "contours_data.json"
with open(path, "r") as f:
    contours_data = json.load(f)

contours_data = [[np.array(cnt) for cnt in sublist] for sublist in contours_data]

RED = 0
BLUE = 1

red_thresh_low = (0,100,100)
red_thresh_high = (5,255,255)

blue_thresh_low = (110,100,100)
blue_thresh_high = (105,255,255)

green_thresh_low = (70,50,100)
green_thresh_high = (90,255,255)

purple_thresh_low = (130,50,130)
purple_thresh_high = (150,255,255)

def compare_contours(contour):
    global contours_data
    best_match = float('inf')
    for sublist in contours_data:
        for stored_contour in sublist:
            match = cv2.matchShapes(contour, stored_contour, cv2.CONTOURS_MATCH_I1, 0.0)
            if match < best_match:
                best_match = match
    return best_match

def is_ramp_below(frame,ball_x1,ball_x2,ball_y1,ball_y2,color):
    ball_region = frame[ball_y1:ball_y2+200, ball_x1:ball_x2]
    ball_region_hsv = cv2.cvtColor(ball_region, cv2.COLOR_BGR2HSV)
    if color == RED:
        mask = cv2.inRange(ball_region_hsv, red_thresh_low, red_thresh_high)
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
        eroded = cv2.erode(mask, kernel, iterations=2)
        red_mask_processed = cv2.dilate(eroded, kernel, iterations=2)
    
        if cv2.countNonZero(red_mask_processed) > 6000:
            return True

    elif color == BLUE:
        mask = cv2.inRange(ball_region_hsv, blue_thresh_low, blue_thresh_high)
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
        eroded = cv2.erode(mask, kernel, iterations=2)
        dilated = cv2.dilate(eroded, kernel, iterations=2)
        if cv2.countNonZero(dilated) > 6000:
            return True

    return False


match_thresh = 1
min_area = 200
max_area = 10000

# for example our team is red
cv2.namedWindow(f"RESULT",cv2.WINDOW_NORMAL)

cap = cv2.VideoCapture("test_vid.mp4")

while True:
    ret, frame = cap.read()
    if not ret:
        break
    frame_hsv = cv2.cvtColor(frame,cv2.COLOR_BGR2HSV)

    coords = []
    
    for j in range(2):
        for u in range(3):
            green_thresh_low_rt = (50,20,140-j*25)
            green_thresh_high_rt = (90,255,255)
            purple_thresh_low_rt = (120,20,140-j*25)
            purple_thresh_high_rt = (160,255,255)

            green_mask = cv2.inRange(frame_hsv, green_thresh_low_rt, green_thresh_high_rt)
            purple_mask = cv2.inRange(frame_hsv, purple_thresh_low_rt, purple_thresh_high_rt)

            combined_mask = cv2.bitwise_or(green_mask, purple_mask)

            kernel = np.ones((4,4), np.uint8)
            combined_mask_eroded = cv2.morphologyEx(combined_mask,cv2.MORPH_OPEN,kernel,iterations=3-u)
            combined_mask_eroded_opened = cv2.erode(combined_mask_eroded,kernel,iterations=3-u)
    

            combined_mask_processed = combined_mask_eroded_opened
            contours,_ = cv2.findContours(combined_mask_processed,cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_SIMPLE)

            for contour in contours:
                match = compare_contours(contour)
                if cv2.contourArea(contour) > min_area and cv2.contourArea(contour) < max_area and match < match_thresh:
                    x,y,w,h = cv2.boundingRect(contour)

                    is_checked = False
                    for coord in coords:
                        mid_x = coord[0] + coord[2] / 2
                        mid_y = coord[1] + coord[3] / 2
                        if x-10 < mid_x < x+10 + w and y-10 < mid_y < y+10 + h:
                            is_checked = True
                            break
                    if not is_checked:
                        if is_ramp_below(frame,x,x+w,y,y+h,RED):
                            coords.append([x,y,w,h])

                    cv2.imshow(f"Mask",combined_mask_processed)
    
    
    sorted_coords = sorted(coords, key=lambda x: x[0])
    result_coords = []
    if sorted_coords:
        result_coords.append(sorted_coords[0])
        for coord in sorted_coords[1:]:
            if coord[1] > result_coords[-1][1]:
                result_coords.append(coord)
       

    for k in range(len(result_coords)):
        cv2.rectangle(frame,(result_coords[k][0],result_coords[k][1]),(result_coords[k][0]+result_coords[k][2],result_coords[k][1]+result_coords[k][3]),(0,255,0),2)
        cv2.putText(frame,str(k+1),(result_coords[k][0],result_coords[k][1]-10),cv2.FONT_HERSHEY_SIMPLEX,0.9,(0,255,0),2)

    cv2.putText(frame, str(len(result_coords)) + " balls in ramp", (30, 30), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
    cv2.imshow(f"RESULT",frame)

    if cv2.waitKey(1) == "q":
        break

cap.release()
cv2.destroyAllWindows()