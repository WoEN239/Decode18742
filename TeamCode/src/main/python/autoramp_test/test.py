import cv2
from sosat import SoSAT
import time

cv2.namedWindow("Result",cv2.WINDOW_NORMAL)

sosat = SoSAT()
frame = cv2.imread("test_images/test4.png")

start = time.time()
detectedArtefacts = sosat.detectArtefacts(frame,1,1000,25,4,1,1)
end = time.time()

processingTime = end-start
print(f"Processing time: {processingTime}")

resultFrame = frame.copy()
for artefact in detectedArtefacts:
    x1,y1,x2,y2,matchScore = artefact["x"],artefact["y"],artefact["x"]+artefact["w"],artefact["y"]+artefact["h"],artefact["matchScore"]
    cv2.rectangle(resultFrame,(x1,y1),(x2,y2),(0,255,0),3)

cv2.imshow("Result",resultFrame)
cv2.waitKey(0)
cv2.destroyAllWindows()