import cv2 
import numpy as np
import time

def mostFrequentColor(frame):
    frameHSV = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
    pixels = frameHSV.reshape(-1, 3)
    uniqueColors, counts = np.unique(pixels, axis=0, return_counts=True)
    mostFrequentIndex = np.argmax(counts)
    top_indices = np.argsort(counts)[-5:][::-1]
    
    return uniqueColors[top_indices]
    

cv2.namedWindow("img", cv2.WINDOW_NORMAL)
img  = cv2.imread("testImages/test13.png")
img = cv2.bilateralFilter(img, d=100, sigmaColor=75, sigmaSpace=75)

start = time.time()

imgHSV = cv2.cvtColor(img,cv2.COLOR_BGR2HSV)

hShift = 0
sGain = 1
vGain = 1

h, s, v = cv2.split(imgHSV)
h = np.clip(h + hShift, 0, 255).astype(np.uint8)
s = np.clip(s * sGain, 0, 255).astype(np.uint8)
v = np.clip(v * vGain, 0, 255).astype(np.uint8)


imgHSV = cv2.merge([h, s, v])
end = time.time()

print(mostFrequentColor(img))
print(end-start)

img = cv2.cvtColor(imgHSV, cv2.COLOR_HSV2BGR)

cv2.imshow("img",img)
cv2.waitKey(0)