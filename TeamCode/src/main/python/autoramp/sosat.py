import cv2
import numpy as np
import time

class SoSAT:

    purpleLowerThreshold = np.array([150, 100, 120])
    purpleUpperThreshold = np.array([179, 255, 255])
    greenLowerThreshold = np.array([40, 50, 0])
    greenUpperThreshold = np.array([100, 205, 240])

    def __init__(self,pathToContours):
        pass

    __wbSimple = cv2.xphoto.createSimpleWB()
    __kernel = np.ones((3,3), np.uint8)


    def __prepareImage(self,frame):

        frame = cv2.bilateralFilter(frame, d=10, sigmaColor=100,sigmaSpace=100)
        frame = self.__wbSimple.balanceWhite(frame)

        return frame
    
    def colorMasks(self,frame,hRange,hIterations,sRange,sIterations,vRange,vIterations, morphCloseIterations, erodeIterations, morphOpenIterations):

        frame = self.__prepareImage(frame)
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