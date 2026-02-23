import cv2
import numpy as np

def nothing(x):
    pass

cv2.namedWindow("Set thresholds",cv2.WINDOW_NORMAL)

cv2.createTrackbar("H Min", "Set thresholds", 0, 179, nothing)
cv2.createTrackbar("H Max", "Set thresholds", 179, 179, nothing)
cv2.createTrackbar("S Min", "Set thresholds", 0, 255, nothing)
cv2.createTrackbar("S Max", "Set thresholds", 255, 255, nothing)
cv2.createTrackbar("V Min", "Set thresholds", 0, 255, nothing)
cv2.createTrackbar("V Max", "Set thresholds", 255, 255, nothing)

frame = cv2.imread("test.jpg")
hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)

while True:
    h_min = cv2.getTrackbarPos("H Min", "Set thresholds")
    h_max = cv2.getTrackbarPos("H Max", "Set thresholds")
    s_min = cv2.getTrackbarPos("S Min", "Set thresholds")
    s_max = cv2.getTrackbarPos("S Max", "Set thresholds")
    v_min = cv2.getTrackbarPos("V Min", "Set thresholds")
    v_max = cv2.getTrackbarPos("V Max", "Set thresholds")

    lower = np.array([h_min, s_min, v_min])
    upper = np.array([h_max, s_max, v_max])

    mask = cv2.inRange(hsv, lower, upper)

    cv2.putText(mask,f"min: ({h_min}, {s_min}, {v_min}) ; max: ({h_max}, {s_max}, {v_max})",(10,30),cv2.FONT_HERSHEY_SIMPLEX,0.7,(255,255,255),2)
    
    cv2.imshow("Set thresholds", mask)

    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cv2.destroyAllWindows()