import cv2
from artefacts_detection import ArtefactsDetection
detector = ArtefactsDetection()
detector.load_contours("contours_data.json")
frame = cv2.imread("test1.jpg")
print("DETECTING ARTEFACTS...")
artefacts_coords = detector.detect_artefacts(frame,match_threshold=50 ,min_area=0,mask_thresh_step=40,mask_iterations=4)
print(artefacts_coords)