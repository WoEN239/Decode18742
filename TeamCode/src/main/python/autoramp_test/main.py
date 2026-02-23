import cv2
from artefacts_detection import ArtefactsDetection
import time

detector = ArtefactsDetection()
detector.load_contours("contours_data.json")
frame = cv2.imread("test.jpg")

start = time.time()
artefacts = detector.detect_artefacts(frame,100,50,25,4)
end = time.time()

processing_time = end-start
print(processing_time)

result_frame = frame.copy()
for artefact in artefacts:
    x,y,w,h = artefact[0],artefact[1],artefact[2],artefact[3]
    cv2.rectangle(result_frame,(x,y),(x+w,y+h),(0,255,0),2)

count = len(artefacts)
cv2.putText(result_frame,f"ARTEFACTS DETECTED: {count}",(30,30),cv2.FONT_HERSHEY_SIMPLEX,0.6,(255,0,0),2)

cv2.imshow("Result",result_frame)
cv2.waitKey(0)