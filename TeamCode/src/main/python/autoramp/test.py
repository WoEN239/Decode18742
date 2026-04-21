import cv2 
from sosat import SoSAT
import time

cv2.namedWindow("frame",cv2.WINDOW_NORMAL)

sosat = SoSAT("")
frame = cv2.imread("testImages/test23.png")

start = time.time()
masks = sosat.colorMasks(frame,10,2,50,2,70,5,3,2,3)
end = time.time()

print(end-start)

for mask in masks:
    maskedImage = cv2.bitwise_and(frame, frame, mask=mask)
    cv2.imshow("frame", maskedImage)
    cv2.waitKey(0)