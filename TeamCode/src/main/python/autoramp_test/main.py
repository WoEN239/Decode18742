import cv2
from artefacts_detection import ArtefactsDetection
detector = ArtefactsDetection()
detector.load_contours("contours_data.json")
frame = cv2.imread("test.jpg")
masks = detector.color_masks(frame)
for mask in masks:
    cv2.imshow("Mask",mask)
    cv2.waitKey(0)
cv2.destroyAllWindows() 