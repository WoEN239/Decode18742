import cv2
import numpy as np

green_thresh_low = (60,50,130)
green_thresh_high = (90,255,255)

purple_thresh_low = (120,20,180)
purple_thresh_high = (160,255,255)

cv2.namedWindow("result",cv2.WINDOW_NORMAL)

for i in range(12):

    frame = cv2.imread(f"test{i+1}.jpg")
    frame_hsv = cv2.cvtColor(frame,cv2.COLOR_BGR2HSV)
    for j in range(5):
        for k in range(3):
            green_thresh_low_rt = (50,20,200-j*25)
            green_thresh_high_rt = (90,255,255)
            purple_thresh_low_rt = (120,20,200-j*25)
            purple_thresh_high_rt = (160,255,255)

            green_mask = cv2.inRange(frame_hsv, green_thresh_low_rt, green_thresh_high_rt)
            purple_mask = cv2.inRange(frame_hsv, purple_thresh_low_rt, purple_thresh_high_rt)

            combined_mask = cv2.bitwise_or(green_mask, purple_mask)

            kernel = np.ones((4,4), np.uint8)
            combined_mask_opened = cv2.morphologyEx(combined_mask,cv2.MORPH_OPEN,kernel,iterations=3)
            combined_mask_opened_eroded = cv2.erode(combined_mask_opened,kernel,iterations=3-k)

            combined_mask_processed = combined_mask_opened_eroded


            cv2.imshow("result",combined_mask_processed)
            cv2.waitKey(0)
cv2.destroyAllWindows()