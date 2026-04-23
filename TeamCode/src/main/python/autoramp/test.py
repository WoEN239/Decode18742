import cv2
from sosat import SoSAT, TeamColor
import time

cv2.namedWindow("Result",cv2.WINDOW_NORMAL)

sosat = SoSAT("contoursData.json")
sosat.teamColor = TeamColor.BLUE
frame = cv2.imread("testImages/test1.png")

start = time.time()
detectedArtifacts = sosat.detectArtifacts(frame)
end = time.time()

processingTime = end-start
print(f"Processing time: {processingTime}")

resultFrame = frame.copy()
for artifact in detectedArtifacts:
    x1,y1,x2,y2,matchScore = artifact["x"],artifact["y"],artifact["x"]+artifact["w"],artifact["y"]+artifact["h"],artifact["matchScore"]
    cv2.rectangle(resultFrame,(x1,y1),(x2,y2),(0,255,0),2)
    cv2.putText(resultFrame,f"{matchScore}",(x1,y1-10),cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)

cv2.imshow("Result",resultFrame)
cv2.waitKey(0)
cv2.destroyAllWindows()