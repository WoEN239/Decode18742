import cv2
import numpy as np
import json

from enum import Enum

class TeamColor(Enum):
    RED = 1
    BLUE = 2

class DetectArtifacts():

    redLowerThreshold = np.array([140, 185, 155])
    redUpperThreshold = np.array([179, 255, 255])
    redExpectedValue = np.array([179,150,220])

    blueLowerThreshold = np.array([80, 20, 55])
    blueUpperThreshold = np.array([130, 255, 255])
    blueExpectedValue = np.array([120,150,220])
    
    referenceLowerThreshold = np.array([])
    referenceUpperThreshold = np.array([])
    referenceExpectedThreshold = np.array([])

    purpleLowerThreshold = np.array([145, 60, 100])
    purpleUpperThreshold = np.array([170, 255, 240])
    greenLowerThreshold = np.array([45, 90, 75])
    greenUpperThreshold = np.array([85, 255, 255])

    class Settings():

        matchThreshold = 0.1

        minArea = 75
        
        hRange = 10
        hIterations = 2

        sRange = 10
        sIterations = 2

        vRange = 75
        vIterations = 5

        morphOpenValue = 0
        morphOpenRange = 0
        morphOpenIterations = 1

        erodeValue = 1
        erodeRange = 0
        erodeIterations = 1

        morphCloseValue = 6
        morphCloseRange = 4
        morphCloseIterations = 2

    def __init__(self,pathToContours):

        with open(pathToContours, "r") as f:

            loaded = json.load(f)
            self.contoursList = [np.array(contour, dtype=np.int32).reshape(-1, 1, 2) for contour in loaded]

        self.__kernel = np.ones((3,3), np.uint8)
        
    def changeTeamColor(self,teamColor):

        if teamColor == TeamColor.BLUE:

            self.imageCorrection.referenceLowerThreshold = self.blueLowerThreshold
            self.imageCorrection.referenceUpperThreshold = self.blueUpperThreshold
            self.imageCorrection.referenceExpectedValue = self.blueExpectedValue

        elif teamColor == TeamColor.RED:

            self.imageCorrection.referenceLowerThreshold = self.redLowerThreshold
            self.imageCorrection.referenceUpperThreshold = self.redUpperThreshold
            self.imageCorrection.referenceExpectedValue = self.redExpectedValue
    
    class imageCorrection():

        referenceLowerThreshold = np.array([])
        referenceUpperThreshold = np.array([])
        referenceExpectedValue = np.array([])

        def removeNoise(frame):

            resultFrame = cv2.bilateralFilter(frame,15,50,50)
            
            return resultFrame

        def correctColor(frameHSV, referenceLowerThreshold,referenceUpperThreshold, referenceExpectedValue):

            referenceMask = cv2.inRange(frameHSV,referenceLowerThreshold,referenceUpperThreshold) > 0

            referencePixels = frameHSV[referenceMask]

            if len(referencePixels) == 0:
                return frameHSV

            meanValue = np.mean(referencePixels, axis=0)

            correction = (
                referenceExpectedValue - meanValue
            ).astype(np.int16)

            frameHSV = frameHSV.astype(np.int16)

            frameHSV += correction

            frameHSV[:,:,0] = np.clip(frameHSV[:,:,0], 0, 179)
            frameHSV[:,:,1:] = np.clip(frameHSV[:,:,1:], 0, 255)

            return frameHSV.astype(np.uint8)
        
    def correctImage(self,frame):

        frame = self.imageCorrection.removeNoise(frame)
        frameHSV = cv2.cvtColor(frame,cv2.COLOR_BGR2HSV)
        frameHSV = self.imageCorrection.correctColor(frameHSV,self.blueLowerThreshold,self.blueUpperThreshold,np.array([120,220,200]))

        return frameHSV
            
    def colorMasks( self,

                    frameHSV,

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

        masks = []

        hStep = hRange / hIterations
        sStep = sRange / sIterations
        vStep = vRange / vIterations

        morphOpenStep = int( (morphOpenRange / morphOpenIterations) )
        erodeStep = int( (erodeRange / erodeIterations) )
        morphCloseStep = int( (morphCloseRange / morphCloseIterations) )

        startLowerThreshold = np.clip(np.array([lowerThreshold[0] + (hRange / 2), lowerThreshold[1] + (sRange / 2), lowerThreshold[2] + (vRange / 2)] ), [0, 0, 0], [179, 255, 255])
        startUpperThreshold = np.clip(np.array([upperThreshold[0] + (hRange / 2), upperThreshold[1] + (sRange / 2), upperThreshold[2] + (vRange / 2)] ), [0, 0, 0], [179, 255, 255])

        startTimeOpenValue = int( morphOpenValue - (morphOpenRange / 2) )
        startTimeErodeValue = int( erodeValue - (erodeRange / 2) )
        startTimeCloseValue = int( morphCloseValue - (morphCloseRange / 2) )

        realTimeLowerThreshold = startLowerThreshold.copy()
        realTimeUpperThreshold = startUpperThreshold.copy()

        realTimeOpenValue = startTimeOpenValue
        realTimeErodeValue = startTimeErodeValue
        realTimeCloseValue = startTimeCloseValue


        for hIteration in range(hIterations):

            realTimeLowerThreshold[0] = np.clip(startLowerThreshold[0] - hStep * hIteration, 0, 179)
            realTimeUpperThreshold[0] = np.clip(startUpperThreshold[0] - hStep * hIteration, 0, 179)

            for sIteration in range(sIterations):

                realTimeLowerThreshold[1] = np.clip(startLowerThreshold[1] - sStep * sIteration, 0, 255)
                realTimeUpperThreshold[1] = np.clip(startUpperThreshold[1] - sStep * sIteration, 0, 255)

                for vIteration in range(vIterations):

                    realTimeLowerThreshold[2] = np.clip(startLowerThreshold[2] - vStep * vIteration, 0, 255)
                    realTimeUpperThreshold[2] = np.clip(startUpperThreshold[2] - vStep * vIteration, 0, 255)

                    for openIteration in range(morphOpenIterations):

                        realTimeOpenValue = max(0, startTimeOpenValue - morphOpenStep * openIteration)

                        for erodeIteration in range(erodeIterations):

                            realTimeErodeValue = max(0, startTimeErodeValue - erodeStep * erodeIteration)

                            for closeIteration in range(morphCloseIterations):
                                
                                realTimeCloseValue = max(0, startTimeCloseValue - morphCloseStep * closeIteration)


                                mask = cv2.inRange(frameHSV, realTimeLowerThreshold.astype(np.uint8), realTimeUpperThreshold.astype(np.uint8))
                            
                                mask = cv2.erode(mask, self.__kernel, iterations=realTimeErodeValue)
                                mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, self.__kernel, iterations=realTimeOpenValue)    
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

    def __isNotPosOverlaps(self,existingObjects,newObject):

        for object in existingObjects:

            newMid = (newObject["x"] + newObject["w"]/2, newObject["y"] + newObject["h"]/2)
            oldMid = (object["x"] + object["w"]/2, object["y"] + object["h"]/2)

            if abs(newMid[0]-oldMid[0]) < (newObject["w"]/2 + object["w"]/2) and abs(newMid[1]-oldMid[1]) < (newObject["h"]/2 + object["h"]/2):
                return False

        return True
    
    def detectObjects(self,frame):
        
        detectedObjects = []

        frameHSV = self.correctImage(frame)

        masks = []

        masks.extend(   self.colorMasks(frameHSV,
                            self.greenLowerThreshold,self.greenUpperThreshold,
                            self.Settings.hRange,self.Settings.hIterations,
                            self.Settings.sRange,self.Settings.sIterations,
                            self.Settings.vRange,self.Settings.vIterations,
                            self.Settings.morphOpenValue,self.Settings.morphOpenRange,self.Settings.morphOpenIterations,
                            self.Settings.erodeValue,self.Settings.erodeRange,self.Settings.erodeIterations,
                            self.Settings.morphCloseValue,self.Settings.morphCloseRange,self.Settings.morphCloseIterations)
                    )
        
        masks.extend(   self.colorMasks(frameHSV,
                            self.purpleLowerThreshold,self.purpleUpperThreshold,
                            self.Settings.hRange,self.Settings.hIterations,
                            self.Settings.sRange,self.Settings.sIterations,
                            self.Settings.vRange,self.Settings.vIterations,
                            self.Settings.morphOpenValue,self.Settings.morphOpenRange,self.Settings.morphOpenIterations,
                            self.Settings.erodeValue,self.Settings.erodeRange,self.Settings.erodeIterations,
                            self.Settings.morphCloseValue,self.Settings.morphCloseRange,self.Settings.morphCloseIterations)
                    )
        
        for mask in masks:

            contours, _ = cv2.findContours(mask, cv2.RETR_LIST, cv2.CHAIN_APPROX_NONE)

            for contour in contours:

                if cv2.contourArea(contour) > self.Settings.minArea:

                    matchScore = self.__matchContours(contour)

                    if matchScore < float(self.Settings.matchThreshold):

                        x,y,w,h = cv2.boundingRect(contour)

                        objectInfo = {
                            "x": x,
                            "y": y,
                            "w": w,
                            "h": h,
                            "matchScore": matchScore
                        }

                        if (self.__isNotPosOverlaps(detectedObjects,objectInfo)):

                            detectedObjects.append(objectInfo)
        
        return detectedObjects