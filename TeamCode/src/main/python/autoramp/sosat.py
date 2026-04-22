import cv2
import numpy as np

import json


class SoSAT:

    contoursList = []

    purpleLowerThreshold = np.array([150, 100, 120])
    purpleUpperThreshold = np.array([179, 255, 255])
    greenLowerThreshold = np.array([40, 50, 0])
    greenUpperThreshold = np.array([100, 205, 240])

    def __init__(self,pathToContours):
        with open(pathToContours, "r") as f:
            loaded = json.load(f)
            self.contoursList = [np.array(contour, dtype=np.int32).reshape(-1, 1, 2) for contour in loaded]

    __wbSimple = cv2.xphoto.createSimpleWB()
    __kernel = np.ones((3,3), np.uint8)


    def __prepareImage(self,frame,brightness=150):
        frame = cv2.bilateralFilter(frame, d=10, sigmaColor=150,sigmaSpace=100)
        frame = self.__wbSimple.balanceWhite(frame)

        frameGray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        frameBrightness =  np.mean(frameGray)
        frame = cv2.convertScaleAbs(frame, alpha=1, beta=brightness - frameBrightness)
        return frame
    
    def colorMasks(self,frame,brightness=150,hRange=10,hIterations=2,sRange=50,sIterations=2,vRange=70,vIterations=5, morphCloseIterations=3, erodeIterations=2, morphOpenIterations=3):

        frame = self.__prepareImage(frame, brightness)
        frameHSV = cv2.cvtColor(frame,cv2.COLOR_BGR2HSV)

        masks = []

        hStep = (hRange / hIterations) / 2
        sStep = (sRange / sIterations) / 2
        vStep = (vRange / vIterations) / 2

        realTimeGreenLowerThreshold = np.clip(np.array([self.greenLowerThreshold[0]+(hRange/2), self.greenLowerThreshold[1]+(sRange/2), self.greenLowerThreshold[2]+(vRange/2)]), [0, 0, 0], [179, 255, 255])
        realTimePurpleLowerThreshold = np.clip(np.array([self.purpleLowerThreshold[0]+(hRange/2), self.purpleLowerThreshold[1]+(sRange/2), self.purpleLowerThreshold[2]+(vRange/2)]), [0, 0, 0], [179, 255, 255])

        for hIteration in range(hIterations):

            if realTimeGreenLowerThreshold[0] - hStep >= 0:
                realTimeGreenLowerThreshold[0] -= hStep
            
            if realTimePurpleLowerThreshold[0] - hStep >= 0:
                realTimePurpleLowerThreshold[0] -= hStep

            for sIteration in range(sIterations):

                realTimeGreenLowerThreshold[1] = np.clip(realTimeGreenLowerThreshold[1] - sStep, 0, 255)
                realTimePurpleLowerThreshold[1] = np.clip(realTimePurpleLowerThreshold[1] - sStep, 0, 255)

                for vIteration in range(vIterations):

                    realTimeGreenLowerThreshold[2] = np.clip(realTimeGreenLowerThreshold[2] - vStep, 0, 255)
                    realTimePurpleLowerThreshold[2] = np.clip(realTimePurpleLowerThreshold[2] - vStep, 0, 255)

                    # Skip if all values already at maximum bounds
                    allMaxed = (realTimeGreenLowerThreshold[2] >= 255 and 
                                realTimePurpleLowerThreshold[2] >= 255)
                    
                    if not allMaxed:
                        greenMask = cv2.inRange(frameHSV, realTimeGreenLowerThreshold.astype(np.uint8), self.greenUpperThreshold.astype(np.uint8))
                        purpleMask = cv2.inRange(frameHSV, realTimePurpleLowerThreshold.astype(np.uint8), self.purpleUpperThreshold.astype(np.uint8))

                    mask = cv2.bitwise_or(greenMask, purpleMask)
                
                    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, self.__kernel, iterations=morphCloseIterations)
                    mask = cv2.erode(mask, self.__kernel, iterations=erodeIterations)
                    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, self.__kernel, iterations=morphOpenIterations)

                    masks.append(mask)

        return masks
    
    def __matchContours(self,contour):
        bestMatch = float('inf')
        for storedContour in self.contoursList:
            match = cv2.matchShapes(contour, storedContour,1,0.0)
            if match < bestMatch:
                bestMatch = match
        return bestMatch

    def __isPosOverlaps(self,existingArtifacts,newArtifact):

        for artifact in existingArtifacts:
            newMid = (newArtifact["x"] + newArtifact["w"]/2, newArtifact["y"] + newArtifact["h"]/2)
            oldMid = (artifact["x"] + artifact["w"]/2, artifact["y"] + artifact["h"]/2)

            if abs(newMid[0]-oldMid[0]) < (newArtifact["w"]/2 + artifact["w"]/2) and abs(newMid[1]-oldMid[1]) < (newArtifact["h"]/2 + artifact["h"]/2):
                return True

        return False
    
    def __detectFromMasks(self, masks, matchThreshold, minArea):
        detectedArtifacts = []
        for mask in masks:
            contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            for contour in contours:
                if cv2.contourArea(contour) > minArea:
                    matchScore = self.__matchContours(contour)
                    if matchScore < float(matchThreshold):
                        x,y,w,h = cv2.boundingRect(contour)
                        artifactInfo = {
                            "x": x,
                            "y": y,
                            "w": w,
                            "h": h,
                            "matchScore": matchScore
                        }
                        if not(self.__isPosOverlaps(detectedArtifacts,artifactInfo)):
                            detectedArtifacts.append(artifactInfo)

        return detectedArtifacts

    def detectArtifacts(self,frame,matchThreshold=0.5,minArea=100,brightness=150,hRange=10,hIterations=2,sRange=50,sIterations=2,vRange=70,vIterations=5, morphCloseIterations=3, erodeIterations=2, morphOpenIterations=3):
        masks = self.colorMasks(frame,brightness,hRange,hIterations,sRange,sIterations,vRange,vIterations,morphCloseIterations,erodeIterations,morphOpenIterations)
        detectedArtifacts = self.__detectFromMasks(masks, matchThreshold, minArea)
        return detectedArtifacts