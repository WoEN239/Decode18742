import cv2
import numpy as np

from sosat import SoSAT
from enum import Enum

class TeamColor(Enum):
    RED = 1
    BLUE = 2

class AutoRamp():

    class Settings():

        matchThreshold = 1

        minArea = 50
        
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

    teamColor = None

    referenceLowerThreshold = np.array([0, 0, 45])
    referenceUpperThreshold = np.array([60, 80, 245])
    referenceExpectedValue = np.array([120,150,220])

    redLowerThreshold = np.array([140, 185, 155])
    redUpperThreshold = np.array([179, 255, 255])
    blueLowerThreshold = np.array([80, 20, 55])
    blueUpperThreshold = np.array([130, 255, 255])
    
    purpleLowerThreshold = np.array([145, 60, 100])
    purpleUpperThreshold = np.array([170, 255, 240])
    greenLowerThreshold = np.array([45, 90, 75])
    greenUpperThreshold = np.array([85, 255, 255])

    def __init__(self,pathToContours):

        self.sosat = SoSAT(pathToContours)

        self.sosat.referenceLowerThershold = self.referenceLowerThreshold
        self.sosat.referenceUpperThreshold = self.referenceUpperThreshold
        self.sosat.referenceExpectedValue = self.referenceExpectedValue

    def changeTeamColor(self,teamColor):

        self.teamColor = teamColor

    def detectArtifacts(self,frame):

        detectedArtifacts = []

        detectedArtifacts.extend(self.sosat.detectObjects(  frame=frame,
                                                            
                                                            minArea=self.Settings.minArea,
                                                            matchThreshold=self.Settings.matchThreshold, 

                                                            lowerThreshold=self.purpleLowerThreshold,
                                                            upperThreshold=self.purpleUpperThreshold,

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
            
