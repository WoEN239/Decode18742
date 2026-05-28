import cv2
import numpy as np

from sosat import SoSAT
from enum import Enum

class TeamColor(Enum):
    RED = 1
    BLUE = 2

class AutoRamp():

    class Settings():

        matchThreshold = 0.1

        minArea = 100
        
        hRange = 5
        hIterations = 1

        sRange = 25
        sIterations = 2

        vRange = 30
        vIterations = 3

        morphOpenValue = 1
        morphOpenRange = 2
        morphOpenIterations = 2

        erodeValue = 0
        erodeRange = 0
        erodeIterations = 0

        morphCloseValue = 2
        morphCloseRange = 0
        morphCloseIterations = 1

    __teamColor = TeamColor.BLUE

    __referenceLowerThershold = np.array([0, 0, 0])
    __referenceUpperThreshold = np.array([179, 255, 255])
    __refernceExpectedValue = np.array([100,255,127])

    redLowerThreshold = np.array([140, 185, 155])
    redUpperThreshold = np.array([179, 255, 255])
    blueLowerThreshold = np.array([80, 20, 55])
    blueUpperThreshold = np.array([130, 255, 255])

    purpleLowerThreshold = np.array([130, 50, 50])
    purpleUpperThreshold = np.array([160, 255, 230])
    greenLowerThreshold = np.array([50, 75, 30])
    greenUpperThreshold = np.array([80, 255, 190])

    def __init__(self,pathToContours):

        self.sosat = SoSAT(pathToContours)

    def changeTeamColor(self,teamColor):
        self.__teamColor = teamColor
        self.sosat.referenceLowerThershold = self.blueLowerThreshold if teamColor == TeamColor.BLUE else self.redLowerThreshold
        self.sosat.referenceUpperThreshold = self.blueUpperThreshold if teamColor == TeamColor.BLUE else self.redUpperThreshold
        self.sosat.referenceExpectedValue = np.array([100,255,127]) if teamColor == TeamColor.BLUE else np.array([160,255,127])

    def detectArtifacts(self,frame):

        detectedArtifacts = []

        detectedArtifacts.extend(self.sosat.detectObjects(  frame=frame,
                                                            
                                                            minArea=self.Settings.minArea,
                                                            matchThreshold=self.Settings.matchThreshold, 

                                                            lowerThreshold=self.greenLowerThreshold,
                                                            upperThreshold=self.greenUpperThreshold,

                                                            hRange=self.Settings.hRange,
                                                            hIterations=self.Settings.hIterations,

                                                            sRange=self.Settings.sRange,
                                                            sIterations=self.Settings.sIterations,

                                                            vRange=self.Settings.vRange,
                                                            vIterations=self.Settings.vIterations,
                                                            
                                                            morphOpenValue=self.Settings.morphOpenValue,
                                                            morphOpenRange=self.Settings.morphOpenRange,
                                                            morphOpenIterations=self.Settings.morphOpenIterations,

                                                            erodeValue=self.Settings.erodeValue,
                                                            erodeRange=self.Settings.erodeRange,
                                                            erodeIterations=self.Settings.erodeIterations,

                                                            morphCloseValue=self.Settings.morphCloseValue,
                                                            morphCloseRange=self.Settings.morphCloseRange,
                                                            morphCloseIterations=self.Settings.morphCloseIterations

                                                            )    
                                )
        
        detectedArtifacts.extend(self.sosat.detectObjects(  frame=frame,
                                                            
                                                            minArea=self.Settings.minArea,
                                                            matchThreshold=self.Settings.matchThreshold, 

                                                            lowerThreshold=self.greenLowerThreshold,
                                                            upperThreshold=self.greenUpperThreshold,

                                                            hRange=self.Settings.hRange,
                                                            hIterations=self.Settings.hIterations,

                                                            sRange=self.Settings.sRange,
                                                            sIterations=self.Settings.sIterations,

                                                            vRange=self.Settings.vRange,
                                                            vIterations=self.Settings.vIterations,
                                                            
                                                            morphOpenValue=self.Settings.morphOpenValue,
                                                            morphOpenRange=self.Settings.morphOpenRange,
                                                            morphOpenIterations=self.Settings.morphOpenIterations,

                                                            erodeValue=self.Settings.erodeValue,
                                                            erodeRange=self.Settings.erodeRange,
                                                            erodeIterations=self.Settings.erodeIterations,

                                                            morphCloseValue=self.Settings.morphCloseValue,
                                                            morphCloseRange=self.Settings.morphCloseRange,
                                                            morphCloseIterations=self.Settings.morphCloseIterations

                                                            )    
                                )

        return detectedArtifacts
            
