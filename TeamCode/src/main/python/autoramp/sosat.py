import cv2
import numpy as np

import json

from enum import Enum

class TeamColor(Enum):
    RED = 1
    BLUE = 2

class SoSAT:

    contoursList = []

    teamColor = TeamColor.BLUE

    redLowerThreshold = np.array([140, 185, 155])
    redUpperThreshold = np.array([179, 255, 255])
    blueLowerThreshold = np.array([80, 20, 55])
    blueUpperThreshold = np.array([130, 255, 255])

    purpleLowerThreshold = np.array([150, 100, 120])
    purpleUpperThreshold = np.array([179, 255, 255])
    greenLowerThreshold = np.array([25, 40, 100])
    greenUpperThreshold = np.array([90, 250, 210])

    def __init__(self,pathToContours):
        with open(pathToContours, "r") as f:
            loaded = json.load(f)
            self.contoursList = [np.array(contour, dtype=np.int32).reshape(-1, 1, 2) for contour in loaded]

    __kernel = np.ones((3,3), np.uint8)
    
    class imageCorrection():
        
        def __init__(self):
            pass

        def removeNoise(self,frame,iterations):
            
            resultFrame = cv2.medianBlur(frame,iterations)
            
            return resultFrame

        def correctColor(self,frameHSV,referenceLowerThershold,referenceUpperThreshold,referenceExpectedValue):

            referenceMask = cv2.inRange(frameHSV,referenceLowerThershold,referenceUpperThreshold) > 0

            referencePixels = frameHSV[referenceMask]

            if len(referencePixels) == 0:
                return frameHSV

            meanValue = np.mean(referencePixels, axis=0)

            correction = (
                referenceExpectedValue - meanValue
            ).astype(np.int16)

            frameHSV = frameHSV.astype(np.int16)

            frameHSV += correction

            return np.clip(frameHSV, 0, 255).astype(np.uint8)
        
        def correctImage(self,frame,referenceLowerThershold,referenceUpperThreshold,referenceExpectedValue,noiseRemovingIterations=3):
            
            frame = self.removeNoise(frame,noiseRemovingIterations)
            frameHSV = cv2.cvtColor(frame,cv2.COLOR_BGR2HSV)
            frameHSV = self.correctColor(frameHSV,referenceLowerThershold,referenceUpperThreshold,referenceExpectedValue)
            
            return cv2.cvtColor(frameHSV,cv2.COLOR_HSV2BGR)
    
    correction = imageCorrection()
    
    def colorMasks(self,frame,hRange=10,hIterations=2,sRange=50,sIterations=2,vRange=70,vIterations=5, morphCloseIterations=3, erodeIterations=2, morphOpenIterations=3):

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
                        x = max(0, x)
                        y = max(0, y)
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

    def detectArtifacts(self,frame,matchThreshold=0.5,minArea=100,hRange=10,hIterations=2,sRange=50,sIterations=2,vRange=70,vIterations=5, morphCloseIterations=3, erodeIterations=2, morphOpenIterations=3):
        
        frame = self.correction.correctImage(frame,self.blueLowerThreshold,self.blueUpperThreshold,np.array([100,255,127]))

        masks = self.colorMasks(frame,hRange,hIterations,sRange,sIterations,vRange,vIterations,morphCloseIterations,erodeIterations,morphOpenIterations)
        detectedArtifacts = self.__detectFromMasks(masks, matchThreshold, minArea)
        
        return detectedArtifacts