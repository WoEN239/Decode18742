import cv2
import numpy as np
import json

class SoSAT:
    greenThreshLower = np.array([74,50,90])
    greenThreshUpper = np.array([88,255,249])
    purpleThreshLower = np.array([119,25,60])
    purpleThreshUpper = np.array([142,251,255])

    morphOpenVal = 2
    morphErodeVal = 2

    contoursList = []

    def __init__(self,contoursPath="contours_data.json"):
        with open(contoursPath, "r") as f:
            loaded = json.load(f)
            self.contoursList = [np.array(contour, dtype=np.int32).reshape(-1, 1, 2) for contour in loaded]


    def colorMasks(self,frame,step=25,iterations=4):
        masks = []
        frame_hsv = cv2.cvtColor(frame,cv2.COLOR_BGR2HSV)

        greenThreshLowerRT = self.greenThreshLower
        purpleThreshLowerRT = self.purpleThreshLower

        for threshIter in range(iterations):
            greenThreshLowerRT[2]-=step
            purpleThreshLowerRT[2]-=step
            maskGreen = cv2.inRange(frame_hsv,greenThreshLowerRT,self.greenThreshUpper)
            maskPurple = cv2.inRange(frame_hsv,purpleThreshLowerRT,self.purpleThreshUpper)

            maskCombined = cv2.bitwise_or(maskGreen,maskPurple)

            kernel = np.ones((3,3),np.uint8)
            maskCombined = cv2.morphologyEx(maskCombined, cv2.MORPH_OPEN, kernel, iterations=self.morphOpenVal)
            maskCombined = cv2.erode(maskCombined, kernel, iterations=self.morphErodeVal)
            

            masks.append(maskCombined)

        return masks

    def __matchContours(self,contour):
        bestMatch = float('inf')
        for storedContour in self.contoursList:
            match = cv2.matchShapes(contour, storedContour,1,0.0)
            if match < bestMatch:
                bestMatch = match
        return bestMatch

    def __isPosOverlaps(self,existingArtefacts,newArtefact):

        for artefact in existingArtefacts:
            newMid = (newArtefact["x"] + newArtefact["w"]/2, newArtefact["y"] + newArtefact["h"]/2)
            oldMid = (artefact["x"] + artefact["w"]/2, artefact["y"] + artefact["h"]/2)

            if abs(newMid[0]-oldMid[0]) < (newArtefact["w"]/2 + artefact["w"]/2) and abs(newMid[1]-oldMid[1]) < (newArtefact["h"]/2 + artefact["h"]/2):
                return True

        return False

    def detectArtefacts(self,frame,matchThreshold=5,minArea=0,maskThreshStep=25,maskIterations=4):
        masks = self.colorMasks(frame,step=maskThreshStep,iterations=maskIterations)

        detectedArtefacts = []
        for mask in masks:
            contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            for contour in contours:
                if cv2.contourArea(contour) > minArea:
                    matchScore = self.__matchContours(contour)
                    if matchScore < float(matchThreshold):
                        x,y,w,h = cv2.boundingRect(contour)
                        artefactInfo = {
                            "x": x,
                            "y": y,
                            "w": w,
                            "h": h,
                            "matchScore": matchScore
                        }
                        if not(self.__isPosOverlaps(detectedArtefacts,artefactInfo)):
                            detectedArtefacts.append(artefactInfo)

        return detectedArtefacts