import cv2
import numpy as np
import json

class SoSAT:
    greenThreshLower = np.array([70,113,120])
    greenThreshUpper = np.array([90,255,249])
    purpleThreshLower = np.array([106,0,0])
    purpleThreshUpper = np.array([140,171,255])

    morphOpenVal = 6
    morphErodeVal = 6
    morphCloseVal = 6
    contoursList = []

    def __init__(self,contoursPath="contours_data.json"):
        with open(contoursPath, "r") as f:
            loaded = json.load(f)
            self.contoursList = [np.array(contour, dtype=np.int32).reshape(-1, 1, 2) for contour in loaded]


    def colorMasks(self,frame,maskValStep=10,maskValIterations=10,maskSatStep=10,maskSatIterations=10,morphStep=1,morphIterations=4):
        masks = []
        frame_hsv = cv2.cvtColor(frame,cv2.COLOR_BGR2HSV)

        greenThreshLowerRT = self.greenThreshLower.copy()
        purpleThreshLowerRT = self.purpleThreshLower.copy()
        greenThreshUpperRT = self.greenThreshUpper.copy()
        purpleThreshUpperRT = self.purpleThreshUpper.copy()

        for valIter in range(maskValIterations):
            for satIter in range(maskSatIterations):
                for openIter in range(morphIterations):
                    for erodeIter in range(morphIterations):
                        for closeIter in range(morphIterations):

                            greenThreshLowerRT[2] = self.greenThreshLower[2] - valIter*maskValStep
                            purpleThreshLowerRT[2] = self.purpleThreshLower[2] - valIter*maskValStep
                            greenThreshUpperRT[2] = self.greenThreshUpper[2] - valIter*maskValStep
                            purpleThreshUpperRT[2] = self.purpleThreshUpper[2] - valIter*maskValStep
                            

                            greenThreshLowerRT[1] = self.greenThreshLower[1] - satIter*maskSatStep
                            purpleThreshLowerRT[1] = self.purpleThreshLower[1] - satIter*maskSatStep
                            greenThreshUpperRT[1] = self.greenThreshUpper[1] - satIter*maskSatStep
                            purpleThreshUpperRT[1] = self.purpleThreshUpper[1] - satIter*maskSatStep

                            greenThreshLowerRT = np.clip(greenThreshLowerRT, 0, 255)
                            purpleThreshLowerRT = np.clip(purpleThreshLowerRT, 0, 255)
                            greenThreshUpperRT = np.clip(greenThreshUpperRT, 0, 255)
                            purpleThreshUpperRT = np.clip(purpleThreshUpperRT, 0, 255)

                            if np.any(greenThreshLowerRT > greenThreshUpperRT) or np.any(purpleThreshLowerRT > purpleThreshUpperRT):
                                continue

                            if self.morphOpenVal-openIter*morphStep < 0 or self.morphErodeVal-erodeIter*morphStep < 0 or self.morphCloseVal-closeIter*morphStep < 0:
                                continue

                            maskGreen = cv2.inRange(frame_hsv,greenThreshLowerRT,greenThreshUpperRT)
                            maskPurple = cv2.inRange(frame_hsv,purpleThreshLowerRT,purpleThreshUpperRT)

                            maskCombined = cv2.bitwise_or(maskGreen,maskPurple)

                            kernel = np.ones((3,3),np.uint8)
                
                            maskCombined = cv2.morphologyEx(maskCombined, cv2.MORPH_CLOSE, kernel, iterations=self.morphCloseVal-closeIter*morphStep)
                            maskCombined = cv2.morphologyEx(maskCombined, cv2.MORPH_OPEN, kernel, iterations=self.morphOpenVal-openIter*morphStep)
                            maskCombined = cv2.morphologyEx(maskCombined, cv2.MORPH_ERODE, kernel, iterations=self.morphErodeVal-erodeIter*morphStep)

                            masks.append(maskCombined)

        return masks

    def _cudaMorph(self, mask_gpu, op, kernel, iterations):
        if hasattr(cv2.cuda, 'createMorphologyFilter'):
            morph_filter = cv2.cuda.createMorphologyFilter(op, cv2.CV_8UC1, kernel, iterations=iterations)
            return morph_filter.apply(mask_gpu)

    def colorMasksGPU(self,frame,maskStep=25,maskIterations=4,morphStep=1,morphIterations=1):
        masks = []
        frame_gpu = cv2.cuda_GpuMat()
        frame_gpu.upload(frame)
        frame_hsv_gpu = cv2.cuda.cvtColor(frame_gpu, cv2.COLOR_BGR2HSV)

        greenThreshLowerRT = self.greenThreshLower.copy()
        purpleThreshLowerRT = self.purpleThreshLower.copy()

        for threshIter in range(maskIterations):
            for openIter in range(morphIterations):
                for erodeIter in range(morphIterations):
                    for closeIter in range(morphIterations):
                        greenThreshLowerRT[2]-=maskStep
                        purpleThreshLowerRT[2]-=maskStep
                        if greenThreshLowerRT[2] < 0 or purpleThreshLowerRT[2] < 0 or self.morphOpenVal-openIter*morphStep < 0 or self.morphErodeVal-erodeIter*morphStep < 0 or self.morphCloseVal-closeIter*morphStep < 0: continue

                        lowG = tuple(int(v) for v in greenThreshLowerRT)
                        highG = tuple(int(v) for v in self.greenThreshUpper)
                        lowP = tuple(int(v) for v in purpleThreshLowerRT)
                        highP = tuple(int(v) for v in self.purpleThreshUpper)

                        maskGreen_gpu = cv2.cuda.inRange(frame_hsv_gpu, lowG, highG)
                        maskPurple_gpu = cv2.cuda.inRange(frame_hsv_gpu, lowP, highP)

                        maskCombined_gpu = cv2.cuda.bitwise_or(maskGreen_gpu, maskPurple_gpu)

                        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3,3))
                        maskCombined_gpu = self._cudaMorph(maskCombined_gpu, cv2.MORPH_OPEN, kernel, iterations=self.morphOpenVal-openIter*morphStep)
                        maskCombined_gpu = self._cudaMorph(maskCombined_gpu, cv2.MORPH_ERODE, kernel, iterations=self.morphErodeVal-erodeIter*morphStep)
                        maskCombined_gpu = self._cudaMorph(maskCombined_gpu, cv2.MORPH_CLOSE, kernel, iterations=self.morphCloseVal-closeIter*morphStep)

                        maskCombined = maskCombined_gpu.download()
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

    def detectArtefacts(self,frame,matchThreshold=1,minArea=1000,maskValStep=25,maskValIterations=4,maskSatStep=10,maskSatIterations=10,morphStep=1,morphIterations=4):
        masks = self.colorMasks(frame,maskValStep,maskValIterations,maskSatStep,maskSatIterations,morphStep,morphIterations)

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

        return detectedArtefacts, masks

    def detectArtefactsGPU(self,frame,matchThreshold=5,minArea=0,maskThreshStep=25,maskIterations=4,morphStep=1,morphIterations=1):
        masks = self.colorMasksGPU(frame,maskStep=maskThreshStep,maskIterations=maskIterations,morphStep=morphStep,morphIterations=morphIterations)

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