import cv2
import numpy as np
import json

contours_list = []

for i in range(94):
    frame_orig = cv2.imread(f"ball_mask{i+1}.png")
    frame_gray = cv2.cvtColor(frame_orig, cv2.COLOR_BGR2GRAY)
    _, frame_binary = cv2.threshold(frame_gray, 127, 255, cv2.CHAIN_APPROX_SIMPLE)
    contours, _ = cv2.findContours(frame_binary, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    min_area = 300
    if contours:
        largest_contour = max(contours, key=cv2.contourArea)
        if cv2.contourArea(largest_contour) > min_area:
            filtered_contours = [largest_contour]
        else:
            filtered_contours = []
    else:
        filtered_contours = []
    frame_contours = cv2.drawContours(frame_orig.copy(), filtered_contours, -1, (0, 255, 0), 3)
    contours_list.append(filtered_contours)

serializable_contours = []

for sublist in contours_list:
    converted_sublist = []
    for cnt in sublist:
        converted_sublist.append(cnt.tolist())
    serializable_contours.append(converted_sublist)

with open("contours_data.json","w") as f:
    json.dump(serializable_contours, f, indent=4)

print("Contours saved to contours_data.json")