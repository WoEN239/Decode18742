import cv2
from sosat import SoSAT
import time

sosat = SoSAT()
frame = cv2.imread("test.jpg")

start = time.time()
artefacts = sosat.detect_artefacts(frame,1,75)
end = time.time()

processing_time = end-start
print(processing_time)

result_frame = frame.copy()
for artefact in artefacts:
    x,y,w,h = artefact["x"],artefact["y"],artefact["w"],artefact["h"]
    cv2.rectangle(result_frame,(x,y),(x+w,y+h),(0,255,0),2)

count = len(artefacts)
cv2.putText(result_frame,f"ARTEFACTS DETECTED: {count}",(30,30),cv2.FONT_HERSHEY_SIMPLEX,0.6,(255,0,0),2)

cv2.imshow("Result",result_frame)
cv2.waitKey(0)