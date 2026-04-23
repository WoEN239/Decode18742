import cv2
from sosat import SoSAT, TeamColor
import time

cv2.namedWindow("Result",cv2.WINDOW_NORMAL)

sosat = SoSAT("contoursData.json")
sosat.teamColor = TeamColor.RED
frame = cv2.imread("testImages/test28.png")

start = time.time()
frame = sosat.rampROI(frame)[0]
end = time.time()

processingTime = end-start
print(f"Processing time: {processingTime}")

cv2.imshow("Result",frame)
cv2.waitKey(0)
cv2.destroyAllWindows() 