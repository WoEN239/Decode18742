import cv2
from sosat import SoSAT, TeamColor
import time

cv2.namedWindow("Result",cv2.WINDOW_NORMAL)

sosat = SoSAT("contoursData.json")
sosat.teamColor = TeamColor.BLUE
frame = cv2.imread("testImages/test2.png")

start = time.time()
preparedImage = sosat.prepareImage(frame,150)
end = time.time()

processingTime = end-start
print(f"Processing time: {processingTime}")

cv2.imshow("Result",preparedImage)
cv2.waitKey(0)
cv2.destroyAllWindows()