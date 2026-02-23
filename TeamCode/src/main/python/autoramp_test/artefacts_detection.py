import cv2
import numpy as np
import json

class ArtefactsDetection:
    green_thresh_lower = np.array([60,60,135])
    green_thresh_upper = np.array([100,255,230])
    purple_thresh_lower = np.array([130,20,100])
    purple_thresh_upper = np.array([160,255,235])

    contours_list = []

    def __init__(self):
        pass

    def load_contours(self,contours_path):
        with open(contours_path, "r") as f:
            loaded = json.load(f)
            self.contours_list = [np.array(contour, dtype=np.int32).reshape(-1, 1, 2) for contour in loaded]

    def color_masks(self,frame,step=25,iterations=4):
        masks = []
        frame_hsv = cv2.cvtColor(frame,cv2.COLOR_BGR2HSV)

        green_thresh_lower_rt = self.green_thresh_lower
        purple_thresh_lower_rt = self.purple_thresh_lower

        for thresh_iter in range(iterations):
            green_thresh_lower_rt[2]-=step
            purple_thresh_lower_rt[2]-=step
            mask_green = cv2.inRange(frame_hsv,green_thresh_lower_rt,self.green_thresh_upper)
            mask_purple = cv2.inRange(frame_hsv,purple_thresh_lower_rt,self.purple_thresh_upper)
            mask_combined = cv2.bitwise_or(mask_green,mask_purple)

            kernel = np.ones((3,3),np.uint8)
            mask_combined = cv2.morphologyEx(mask_combined, cv2.MORPH_OPEN, kernel, iterations=1)
            mask_combined = cv2.erode(mask_combined, kernel, iterations=2)
            mask_combined = cv2.morphologyEx(mask_combined, cv2.MORPH_CLOSE, kernel, iterations=3)
            

            masks.append(mask_combined)

        return masks

    def match_contours(self,contour):
        best_match = float('inf')
        for stored_contour in self.contours_list:
            match = cv2.matchShapes(contour, stored_contour,1,0.0)
            if match < best_match:
                best_match = match
        return best_match

    def detect_artefacts(self,frame,match_threshold=5,min_area=0,mask_thresh_step=25,mask_iterations=4):
        masks = self.color_masks(frame,step=mask_thresh_step,iterations=mask_iterations)
        detected_artefacts_coords = []
        for mask in masks:
            contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            for contour in contours:
                if cv2.contourArea(contour) > min_area:
                    match_score = self.match_contours(contour)
                    if match_score < float(match_threshold):
                        x,y,w,h = cv2.boundingRect(contour)

                        is_redetected = False
                        for artefact in detected_artefacts_coords:
                            if abs(artefact[0]-x) < 5 or abs(artefact[1]-y) < 5:
                                is_redetected = True
                                break

                        if not(is_redetected):
                            detected_artefacts_coords.append([x,y,w,h,match_score])

        return detected_artefacts_coords