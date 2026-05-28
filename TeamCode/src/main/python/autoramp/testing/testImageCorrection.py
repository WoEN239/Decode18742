import cv2
import numpy as np

import time

import os


import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

from sosat import SoSAT
from autoramp import AutoRamp, TeamColor

sosat = SoSAT("contoursData.json")
correction = sosat.imageCorrection()
ar = AutoRamp("contoursData.json")
ar.changeTeamColor(TeamColor.BLUE)

frame = cv2.imread("testImages/test1.png")

cv2.imshow("Original Frame", frame)
cv2.waitKey(0)
cv2.destroyAllWindows()

startTime = time.time()
correctedFrame = correction.correctImage(frame, ar.blueLowerThreshold, ar.blueUpperThreshold, np.array([100,255,127]))
endTime = time.time()


print(f"Processed in {endTime - startTime} secs")

cv2.imshow("Corrected Frame", correctedFrame)
cv2.waitKey(0)
cv2.destroyAllWindows()
