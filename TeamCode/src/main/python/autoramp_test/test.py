import cv2
from sosat import SoSAT
import time

cv2.namedWindow("Result",cv2.WINDOW_NORMAL)

sosat = SoSAT()
frame = cv2.imread("test_images/only_in_ramp/test5_6.png")

start = time.time()
detectedArtefacts,masks = sosat.detectArtefacts(frame,0.3,1000,10,5,5,2,2,2)
end = time.time()

processingTime = end-start
print(f"Processing time: {processingTime}")

resultFrame = frame.copy()
for artefact in detectedArtefacts:
    x1,y1,x2,y2,matchScore = artefact["x"],artefact["y"],artefact["x"]+artefact["w"],artefact["y"]+artefact["h"],artefact["matchScore"]
    cv2.rectangle(resultFrame,(x1,y1),(x2,y2),(0,255,0),2)
    cv2.putText(resultFrame,f"{matchScore}",(x1,y1-20),cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)

cv2.imshow("Result",resultFrame)
cv2.waitKey(0)
cv2.destroyAllWindows()