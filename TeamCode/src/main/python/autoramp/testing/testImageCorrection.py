import cv2
import numpy as np

import time

import os


import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

from detection import DetectArtifacts,TeamColor


detect = DetectArtifacts("contoursData.json")
detect.changeTeamColor(TeamColor.BLUE)

frame = cv2.imread("testImages/test1.png")


startTime = time.time()

frame = cv2.cvtColor(detect.correctImage(frame),cv2.COLOR_HSV2BGR)

endTime = time.time()

print(f"Processed in {(endTime - startTime)*1000:.0f} ms")

cv2.imshow("Corrected", frame)
cv2.waitKey(0)
cv2.destroyAllWindows()
