import cv2
import numpy as np
import json

class ArtefactsDetection:
    green_thresh_lower = np.array([50,5,150])
    green_thresh_upper = np.array([90,255,255])
    purple_thresh_lower = np.array([120,5,150])
    purple_thresh_upper = np.array([160,255,255])

    def __init__(self,contours_path="contours_data.json"):
        with open(contours_path,"r") as f:
            self.contours_list = json.load(f)
            f.close()

    def _mask_color(self,frame,step=25,iterations=4):
        masks = []

        frame_hsv = cv2.cvtColor(frame,cv2.BGR2HSV)

        green_thresh_lower_rt = self.green_thresh_lower
        purple_thresh_lower_rt = self.purple_thresh_lower

        for thresh_iter in range(iterations):
            green_thresh_lower_rt[2]-=step
            purple_thresh_lower_rt[2]-=step
            mask_green = cv2.inRange(frame_hsv,green_thresh_lower_rt,self.green_thresh_upper)
            mask_purple = cv2.inRange(frame_hsv,purple_thresh_lower_rt,self.purple_thresh_upper)
            mask_combined = cv2.bitwise_or(mask_green,mask_purple)

            mask_combined = cv2.morphologyEx(mask_combined, cv2.MORPH_OPEN, np.ones((4,4),np.uint8), iterations=3)
            mask_combined = cv2.erode(mask_combined, np.ones((4,4),np.uint8), iterations=4)
            mask_combined = cv2.morphologyEx(mask_combined, cv2.MORPH_CLOSE, np.ones((4,4),np.uint8), iterations=1)

            masks.append(mask_combined)

        return masks

    def _match_contours(self,contour):
        best_match = float('inf')
        for stored_contour in self.contours_list:
            match = cv2.matchShapes(contour, stored_contour, cv2.CONTOURS_MATCH_I1, 0.0)
            if match < best_match:
                best_match = match
        return best_match

    def detect_artefacts(self,frame,match_threshold=0.1,min_area=500,mask_thresh_step=25,mask_iterations=4):
        masks = self._mask_color(frame,step=mask_thresh_step,iterations=mask_iterations)
        detected_artefacts = []
        for mask in masks:
            contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            for contour in contours:
                if cv2.contourArea(contour) > min_area:
                    match_score = self._match_contours(contour)
                    if match_score < match_threshold:
                        detected_artefacts.append(contour)
        return detected_artefacts