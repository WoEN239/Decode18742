import cv2
import numpy as np

import time

import os


import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))

from autoramp import AutoRamp, TeamColor


ar = AutoRamp("contoursData.json")
ar.changeTeamColor(TeamColor.BLUE)

frame = cv2.imread("testImages/test1.png")

startTime = time.time()
detectedArtifacts = ar.detectArtifacts(frame)
endTime = time.time()

print(f"Processed in {(endTime - startTime)*1000:.0f} ms")

for artifact in detectedArtifacts:
    x, y, w, h, matchScore = artifact["x"], artifact["y"], artifact["w"], artifact["h"], artifact["matchScore"]
    cv2.rectangle(frame, (x, y), (x + w, y + h), (0, 255, 0), 2)
    cv2.putText(frame, f"{matchScore:.5f}", (x, y - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)

cv2.imshow("Detected Artifacts", frame)
cv2.waitKey(0)
cv2.destroyAllWindows()
