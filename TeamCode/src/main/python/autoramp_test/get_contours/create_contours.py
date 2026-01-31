import json
import cv2
import numpy as np
import time

cv2.namedWindow("Set contour", cv2.WINDOW_NORMAL)
is_drawing = False
x1 = y1 = x2 = y2 = 0
roi_selected = False
display_frame = None

def wait_for_key_release():
    """Wait for a key press AND release before returning"""
    key = cv2.waitKey(0)
    time.sleep(0.2)  # Wait for key to be physically released
    # Flush any pending key presses
    while cv2.waitKey(1) != -1:
        pass
    return key

def mouse_callback(event, x, y, flags, param):
    global is_drawing, x1, y1, x2, y2, roi_selected, display_frame
    if event == cv2.EVENT_LBUTTONDOWN:
        is_drawing = True
        x1, y1 = x, y
    elif event == cv2.EVENT_MOUSEMOVE:
        if is_drawing and display_frame is not None:
            modif_frame = display_frame.copy()
            cv2.rectangle(modif_frame, (x1, y1), (x, y), (0, 255, 0), 3)
            cv2.imshow("Set contour", modif_frame)
    elif event == cv2.EVENT_LBUTTONUP:
        is_drawing = False
        x2, y2 = x, y
        roi_selected = True

cv2.setMouseCallback("Set contour", mouse_callback)

green_thresh_low = (50, 20, 170)
green_thresh_high = (90, 255, 255)
purple_thresh_low = (120, 20, 170)
purple_thresh_high = (160, 255, 255)
min_area = 500

contours_list = []

for i in range(12):
    full_frame = cv2.imread(f"test{i+1}.jpg")
    if full_frame is None:
        print(f"Could not load test{i+1}.jpg")
        continue
    
    roi_selected = False
    x1 = y1 = x2 = y2 = 0
    display_frame = full_frame
    cv2.imshow("Set contour", full_frame)
    
    while not roi_selected:
        key = cv2.waitKey(30)
        if key == ord('q'):
            break
        elif key == ord("n"):
            break
    
    if key == ord('q'):
        break
    
    if not roi_selected:
        continue
    
    x_min, x_max = min(x1, x2), max(x1, x2)
    y_min, y_max = min(y1, y2), max(y1, y2)
    
    if x_max - x_min < 10 or y_max - y_min < 10:
        print("ROI too small, skipping...")
        continue
    
    user_selected = full_frame[y_min:y_max, x_min:x_max]
    frame_hsv = cv2.cvtColor(user_selected, cv2.COLOR_BGR2HSV)
    
    frame_contours = []  # Store contours for this frame
    
    # Try different parameters and let user select
    for j in range(3):
        for k in range(2):
            green_thresh_low_rt = (50, 20, 170 - j*25)
            green_thresh_high_rt = (90, 255, 255)
            purple_thresh_low_rt = (120, 20, 170 - j*25)
            purple_thresh_high_rt = (160, 255, 255)
            
            green_mask = cv2.inRange(frame_hsv, green_thresh_low_rt, green_thresh_high_rt)
            purple_mask = cv2.inRange(frame_hsv, purple_thresh_low_rt, purple_thresh_high_rt)
            combined_mask = cv2.bitwise_or(green_mask, purple_mask)
            
            kernel = np.ones((4, 4), np.uint8)
            combined_mask_opened = cv2.morphologyEx(combined_mask, cv2.MORPH_OPEN, kernel, iterations=4)
            combined_mask_processed = cv2.erode(combined_mask_opened, kernel, iterations=2-k)
            
            contours, _ = cv2.findContours(combined_mask_processed, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
            filtered_contours = [c for c in contours if min_area < cv2.contourArea(c) < 5000]
            
            if filtered_contours:
                result_frame = user_selected.copy()
                # Draw previously accepted contours in blue
                for prev_cnt in frame_contours:
                    cv2.drawContours(result_frame, [prev_cnt], -1, (255, 0, 0), 2)
                # Draw current contours in green
                cv2.drawContours(result_frame, filtered_contours, -1, (0, 255, 0), 2)
                cv2.imshow("Set contour", result_frame)
                print(f"j={j}, k={k}. Press 'y' accept, 'n' skip, 'd' done with frame")
                
                key = wait_for_key_release()
                if key == ord("y"):
                    frame_contours.extend(filtered_contours)
                    print(f"  -> Added {len(filtered_contours)} contours (total: {len(frame_contours)})")
                    
                    # Show accumulated contours and wait for next action
                    result_frame = user_selected.copy()
                    for cnt in frame_contours:
                        cv2.drawContours(result_frame, [cnt], -1, (255, 0, 0), 2)
                    cv2.imshow("Set contour", result_frame)

                    cv2.waitKey(500)  # Brief pause to view accumulated contours
                    
                elif key == ord("d"):
                    break
                elif key == ord("n"):
                    continue
            
        if key == ord("d"):
            break
    
    if frame_contours:
        contours_list.extend(frame_contours)
        print(f"Frame {i}: Saved {len(frame_contours)} total contours")
    
    if key == ord('q'):
        break

cv2.destroyAllWindows()

# Save contours
serializable_contours = []
for cnt in contours_list:
    points = cnt.reshape(-1, 2).tolist()
    serializable_contours.append(points)

with open("contours_data.json", "w") as f:
    json.dump(serializable_contours, f, indent=4)

print(f"Total contours saved: {len(serializable_contours)}")