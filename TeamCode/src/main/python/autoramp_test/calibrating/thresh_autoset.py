import cv2
from sosat import SoSAT
import numpy as np

def nothing(x):
    pass

sosat = SoSAT()

h_min = 0
h_max = 0
s_min = 0
s_max = 0
v_min = 0
v_max = 0
open_val = 0
erode_val =0

results = np.zeros((25, 25, 25, 25, 25, 25, 5, 5), dtype=np.uint8)
real_num_of_artefacts = 3

frame = cv2.imread("test_actual.png")
hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)

good_results = []

for h_min in range(110,130,10):
    for h_max in range(130,150,10):
        for s_min in range(0,250,25):
            for s_max in range(0,250,25):
                for v_min in range(0,250,5):
                    for v_max in range(0,250,5):
                        for open_val in range(0,5):
                            for erode_val in range(0,5):
                                if h_min >= h_max or s_min >= s_max or v_min >= v_max or h_max-h_min > 30:
                                    continue
                                lower = np.array([h_min, s_min, v_min])
                                upper = np.array([h_max, s_max, v_max])

                                mask = cv2.inRange(hsv, lower, upper)
                                kernel = np.ones((3,3),np.uint8)
                                mask = cv2.erode(mask, kernel, iterations=erode_val)
                                mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel, iterations=open_val)


                                artefacts = sosat.detect_artefacts_on_mask(mask,1,100)

                                if artefacts and len(artefacts) == real_num_of_artefacts:
                                    good_results.append((h_min, h_max, s_min, s_max, v_min, v_max, open_val, erode_val))

                                print(f"Checked: H({h_min}-{h_max}), S({s_min}-{s_max}), V({v_min}-{v_max}), OPEN({open_val}), ERODE({erode_val}) - Detected artefacts: {len(artefacts)}")


print("Good results (h_min, h_max, s_min, s_max, v_min, v_max, open_val, erode_val):")
for result in good_results:
    print(result)

cv2.destroyAllWindows()