from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

from keras.models import load_model, Model, model_from_json
import tensorflow as tf
from scipy import misc
import cv2
import numpy as np
import argparse
import facenet
import detect_face
import os
import sys
import pickle
from glob import glob
from time import sleep
from sklearn.svm import SVC

import firebase_admin
from firebase_admin import credentials
from firebase_admin import db
#import tensorflow as tf


cred = credentials.Certificate('path/to/serviceAccountKey2.json')
firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://finalproject-205218.firebaseio.com/'
    })
ref = db.reference('restricted_access/secret_document')

# datadir=sys.argv[1]
# bgr_image= cv2.imread(datadir)
# rgb_image=cv2.cvtColor(bgr_image,cv2.COLOR_BGR2RGB)

print('Creating networks and loading parameters')
with tf.Graph().as_default():
    gpu_options = tf.GPUOptions(per_process_gpu_memory_fraction=0.6)
    sess = tf.Session(config=tf.ConfigProto(gpu_options=gpu_options, log_device_placement=False))
    with sess.as_default():

        # emotion classify
        with open('./models/xception/model2.json', 'r') as json_file:
            loaded_model_json = json_file.read()
        e_model = model_from_json(loaded_model_json)
        e_model.load_weights('models/xception/weights2.24_0.86.h5')
        
        pnet, rnet, onet = detect_face.create_mtcnn(sess, './')

        minsize = 20  # minimum size of face
        threshold = [0.6, 0.7, 0.7]  # three steps's threshold
        factor = 0.709  # scale factor
        margin = 44
        frame_interval = 3
        batch_size = 1000
        image_size = 182
        input_image_size = 160
        
        print('Loading feature extraction model')
        modeldir = './20170512-110547/20170512-110547.pb'
        facenet.load_model(modeldir)

        images_placeholder = tf.get_default_graph().get_tensor_by_name("input:0")
        embeddings = tf.get_default_graph().get_tensor_by_name("embeddings:0")
        phase_train_placeholder = tf.get_default_graph().get_tensor_by_name("phase_train:0")
        embedding_size = embeddings.get_shape()[1]

        count=0
        while True:
                
                num_of_files = len(glob('./UserData/TryToUnlock/*'))
                if num_of_files >= 1:
                    HumanNames=os.listdir("./UserData/Users/")
                    HumanNames.sort()
                    start = cv2.getTickCount()
                    classifier_filename = './classifier_output/ex.pkl'
                    classifier_filename_exp = os.path.expanduser(classifier_filename)
                    with open(classifier_filename_exp, 'rb') as infile:
                        (model, class_names) = pickle.load(infile)
                        #print('load classifier file-> %s' % classifier_filename_exp)

                    time = (cv2.getTickCount() - start) / cv2.getTickFrequency() * 1000
                    print ('load time : %.3f ms'%time)
                    start = cv2.getTickCount()
                    file_list = glob('./UserData/TryToUnlock/*')
                    bgr_image= cv2.imread(file_list[0])
                    rgb_image=cv2.cvtColor(bgr_image,cv2.COLOR_BGR2RGB)

                    print('Start Recognition!')
                    prevTime = 0

                    bounding_boxes, _ = detect_face.detect_face(rgb_image, minsize, pnet, rnet, onet, threshold, factor)
                    nrof_faces = bounding_boxes.shape[0]
                    time = (cv2.getTickCount() - start) / cv2.getTickFrequency() * 1000
                    print ('detect time : %.3f ms'%time)
        
                    if nrof_faces > 0:
                        det = bounding_boxes[:, 0:4]
                        img_size = np.asarray(rgb_image.shape)[0:2]

                        cropped = []
                        scaled = []
                        scaled_reshape = []
                        bb = np.zeros((nrof_faces,4), dtype=np.int32)

                        for i in range(nrof_faces):
                            emb_array = np.zeros((1, embedding_size))

                            bb[i][0] = det[i][0]
                            bb[i][1] = det[i][1]
                            bb[i][2] = det[i][2]
                            bb[i][3] = det[i][3]

                            start = cv2.getTickCount()
                            # inner exception
                            if bb[i][0] <= 0 or bb[i][1] <= 0 or bb[i][2] >= len(rgb_image) or bb[i][3] >= len(rgb_image):
                                print('face is inner of range!')
                                continue
                                
                            cropped.append(rgb_image[bb[i][1]:bb[i][3], bb[i][0]:bb[i][2], :])
                            cropped[i] = facenet.flip(cropped[i], False)
                            scaled.append(misc.imresize(cropped[i], (image_size, image_size), interp='bilinear'))
                            scaled[i] = cv2.resize(scaled[i], (input_image_size,input_image_size),
                                                    interpolation=cv2.INTER_CUBIC)
                            scaled[i] = facenet.prewhiten(scaled[i])
                            scaled_reshape.append(scaled[i].reshape(-1,input_image_size,input_image_size,3))
                            feed_dict = {images_placeholder: scaled_reshape[i], phase_train_placeholder: False}
                            emb_array[i, :] = sess.run(embeddings, feed_dict=feed_dict)
        
                            time = (cv2.getTickCount() - start) / cv2.getTickFrequency() * 1000
                            print ('embedding time : %.3f ms'%time)

                            start = cv2.getTickCount()  
                            # vector
                            emb_compare=np.loadtxt('emb.out')
                            dist = []
                            unknown = True

                            for j in range(len(class_names)-1):
                                dist.append(j)
                                dist[j] = np.linalg.norm(emb_compare[j]-emb_array[0])
                                if dist[j]<0.7:
                                    unknown = False
                                    break
                                        
                            if unknown == True:
                                ref = db.reference('tryUnlock')
                                ref.set({
                                    'name' : 'unknown'
                                    })
                                print("紐⑤쫫")

                            if unknown == False:
                                predictions = model.predict_proba(emb_array)
                                best_class_indices = np.argmax(predictions, axis=1)
                                best_class_probabilities = predictions[np.arange(len(best_class_indices)), best_class_indices]
                           
                                ## e_model         
                                resized_img = cv2.resize(cropped[i], (48,48))
                                img_bgr_float = resized_img.astype(np.float32)
                                img_bgr_float_normalized = 2*(img_bgr_float - 128)/255

                                ## make gray 3ch for xception network
                                img_cropgray_1ch = cv2.cvtColor(img_bgr_float_normalized, cv2.COLOR_BGR2GRAY)
                                img_cropgray_3ch = cv2.cvtColor(img_cropgray_1ch, cv2.COLOR_GRAY2BGR)
                                img_input = np.expand_dims(img_cropgray_3ch, 0)

                                emotion_prediction = e_model.predict(img_input, 1)
                                emotion_label_arg = np.argmax(emotion_prediction[0])
                                emotion_labels = {0:'Not Smile',1:'Smile'}
                                emotion_text = emotion_labels[emotion_label_arg]
                    
                                for H_i in HumanNames:
                                    if HumanNames[best_class_indices[0]] == H_i:
                                        result_names = HumanNames[best_class_indices[0]]  
                                #print(result_names)
                                time = (cv2.getTickCount() - start) / cv2.getTickFrequency() * 1000
                                print ('emotion time : %.3f ms'%time)

                                ref = db.reference('tryUnlock')
                                ref.set({
                                    'name' : result_names,
                                    'smile' : emotion_text
                                    })

                                if emotion_text == 'Smile':
                                    ref = db.reference('unlock')
                                    ref.set({
                                        'signal': 'true'
                                        })

                    else:
                        print('Unable to align')

                    print('End Recognition!')
                    os.system("rm %s"%file_list[0])
        
                else:
                    sleep(0.0001)
