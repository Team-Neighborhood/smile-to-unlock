import os
import cv2
from glob import glob
from time import sleep

dir='./UserData/Users/'
while True:
    num_of_files = len(glob(dir+'*.jpg'))
    print(num_of_files)
    if num_of_files == 6:
        start = cv2.getTickCount()
        file_list = glob(dir+'*.jpg')
        name=file_list[0].split('_')[0]
        os.system("mkdir "+name)
        for i in range(num_of_files):
            os.system("mv "+file_list[i]+" "+name)
        os.system("rm -rf alignment_output/* ")
        os.system("python3 make_aligndata.py")
        os.system("python3 make_classifier.py")        
        time = (cv2.getTickCount() - start) / cv2.getTickFrequency() * 1000
        print ('detect time : %.3f ms'%time)
    else:
        sleep(2)
