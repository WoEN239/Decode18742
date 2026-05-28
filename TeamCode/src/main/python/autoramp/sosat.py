import cv2
import numpy as np

import json

class SoSAT:

    contoursList = []

    def __init__(self,pathToContours):
        with open(pathToContours, "r") as f:
            loaded = json.load(f)
            self.contoursList = [np.array(contour, dtype=np.int32).reshape(-1, 1, 2) for contour in loaded]

    __kernel = np.ones((3,3), np.uint8)
    
    referenceLowerThershold = np.array([0, 0, 0])
    referenceUpperThreshold = np.array([179, 255, 255])
    referenceExpectedValue = np.array([0,0,0])

    class imageCorrection():

        def __init__(self):
            pass

        noiseRemovingIterations=5

        def removeNoise(self,frame,iterations):
            
            if iterations % 2 == 0:
                iterations += 1

            resultFrame = cv2.medianBlur(frame,iterations)
            
            return resultFrame

        def correctColor(self,frameHSV, referenceLowerThershold,referenceUpperThreshold, referenceExpectedValue):

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
            
        def correctImage(self,frame, referenceLowerThershold,referenceUpperThreshold, referenceExpectedValue):
            
            frame = self.removeNoise(frame,self.noiseRemovingIterations)
            frameHSV = cv2.cvtColor(frame,cv2.COLOR_BGR2HSV)
            frameHSV = self.correctColor(frameHSV, referenceLowerThershold, referenceUpperThreshold, referenceExpectedValue)

            return cv2.cvtColor(frameHSV,cv2.COLOR_HSV2BGR)
    
    correction = imageCorrection()
    
    def colorMasks( self,

                    frame,

                    lowerThreshold,
                    upperThreshold,
                    
                    hRange,
                    hIterations,
                    
                    sRange,
                    sIterations,
                    
                    vRange,
                    vIterations, 
                    
                    morphOpenValue,
                    morphOpenRange,
                    morphOpenIterations,

                    erodeValue,
                    erodeRange,
                    erodeIterations,

                    morphCloseValue,
                    morphCloseRange,
                    morphCloseIterations

                    ):

        frameHSV = cv2.cvtColor(frame,cv2.COLOR_BGR2HSV)

        masks = []

        hStep = (hRange / hIterations) / 2
        sStep = (sRange / sIterations) / 2
        vStep = (vRange / vIterations) / 2

        morphOpenStep = round((morphOpenIterations / hIterations) / 2)
        erodeStep = round((erodeIterations / hIterations) / 2)
        morphCloseStep = round((morphCloseIterations / hIterations) / 2)

        realTimeLowerThreshold = np.clip(np.array([lowerThreshold[0]+(hRange/2), lowerThreshold[1]+(sRange/2), lowerThreshold[2]+(vRange/2)]), [0, 0, 0], [179, 255, 255])
        realTimeOpenValue = morphOpenValue
        realTimeErodeValue = erodeValue
        realTimeCloseValue = morphCloseValue

        for hIteration in range(hIterations):

            if realTimeLowerThreshold[0] - hStep >= 0:
                realTimeLowerThreshold[0] -= hStep

            for sIteration in range(sIterations):

                realTimeLowerThreshold[1] = np.clip(realTimeLowerThreshold[1] - sStep, 0, 255)

                for vIteration in range(vIterations):

                    realTimeLowerThreshold[2] = np.clip(realTimeLowerThreshold[2] - vStep, 0, 255)

                    for openIteration in range(morphOpenIterations):

                        realTimeOpenValue += morphOpenStep

                        for erodeIteration in range(erodeIterations):

                            realTimeErodeValue += erodeStep

                            for closeIteration in range(morphCloseIterations):
                                
                                realTimeCloseValue += morphCloseStep

                                allMaxed = (realTimeLowerThreshold[2] >= 255)
                                
                                if not allMaxed:
                                    mask = cv2.inRange(frameHSV, realTimeLowerThreshold.astype(np.uint8), upperThreshold.astype(np.uint8))
                                
                                    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, self.__kernel, iterations=realTimeOpenValue)
                                    mask = cv2.erode(mask, self.__kernel, iterations=realTimeErodeValue)
                                    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, self.__kernel, iterations=realTimeCloseValue)
                                
                                    masks.append(mask)

        return masks
    
    def __matchContours(self,contour):
        bestMatch = float('inf')
        for storedContour in self.contoursList:
            match = cv2.matchShapes(contour, storedContour,1,0.0)
            if match < bestMatch:
                bestMatch = match
        return bestMatch

    def __isPosOverlaps(self,existingObjects,newObject):

        for object in existingObjects:

            newMid = (newObject["x"] + newObject["w"]/2, newObject["y"] + newObject["h"]/2)
            oldMid = (object["x"] + object["w"]/2, object["y"] + object["h"]/2)

            if abs(newMid[0]-oldMid[0]) < (newObject["w"]/2 + object["w"]/2) and abs(newMid[1]-oldMid[1]) < (newObject["h"]/2 + object["h"]/2):
                return True

        return False
    
    def detectFromMasks(self, masks, matchThreshold, minArea):
        detectedObjects = []
        for mask in masks:

            contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

            for contour in contours:

                if cv2.contourArea(contour) > minArea:

                    matchScore = self.__matchContours(contour)

                    if matchScore < float(matchThreshold):

                        x,y,w,h = cv2.boundingRect(contour)

                        x = max(0, x)
                        y = max(0, y)

                        objectInfo = {
                            "x": x,
                            "y": y,
                            "w": w,
                            "h": h,
                            "matchScore": matchScore
                        }

                        if not(self.__isPosOverlaps(detectedObjects,objectInfo)):
                            detectedObjects.append(objectInfo)

        return detectedObjects

    def detectObjects(  self,
                        frame,

                        lowerThreshold,
                        upperThreshold,

                        matchThreshold,

                        minArea,

                        hRange,
                        hIterations,
                        
                        sRange,
                        sIterations,
                        
                        vRange,
                        vIterations, 
                        
                        morphOpenValue,
                        morphOpenRange,
                        morphOpenIterations,

                        erodeValue,
                        erodeRange,
                        erodeIterations,

                        morphCloseValue,
                        morphCloseRange,
                        morphCloseIterations
                    
                        ):
        
        frame = self.correction.correctImage(frame,self.referenceLowerThershold,self.referenceUpperThreshold,self.referenceExpectedValue)

        masks = self.colorMasks(frame,

                                lowerThreshold,
                                upperThreshold,

                                hRange,
                                hIterations,

                                sRange,
                                sIterations,

                                vRange,
                                vIterations,

                                morphOpenValue,
                                morphOpenRange,
                                morphOpenIterations,

                                erodeValue,
                                erodeRange,
                                erodeIterations,

                                morphCloseValue,
                                morphCloseRange,
                                morphCloseIterations
                                
                                )
        detectedObjects = self.detectFromMasks(masks, matchThreshold, minArea)
        
        return detectedObjects