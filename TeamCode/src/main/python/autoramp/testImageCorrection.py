import cv2
from sosat import SoSAT, TeamColor
import numpy as np
import time

cv2.namedWindow("Result",cv2.WINDOW_NORMAL)

sosat = SoSAT("contoursData.json")
frame = cv2.imread("testImages/test1.png")

start = time.time()
preparedImage = sosat.correction.correctImage(frame,sosat.blueLowerThreshold,sosat.blueUpperThreshold,np.array([100,255,127]))
end = time.time()

processingTime = end-start
print(f"Processing time: {processingTime}")

cv2.imshow("Result",frame)
cv2.waitKey(0)

cv2.imshow("Result",preparedImage)
cv2.waitKey(0)
cv2.destroyAllWindows()