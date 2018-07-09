# -*- coding: utf-8 -*- 

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import tensorflow as tf
import numpy as np
import argparse
import facenet
import detect_face
import os
import sys
import math
import pickle
from sklearn.svm import SVC

    
gpu_options = tf.GPUOptions(per_process_gpu_memory_fraction=0.5)
sess = tf.Session(config=tf.ConfigProto(gpu_options=gpu_options, log_device_placement=False))
with sess.as_default():

    with tf.Session() as sess:
        os.system("rm ./classifier_output/ex.pkl")
        datadir = './alignment_output/'
        dataset = facenet.get_dataset(datadir)
        paths, labels = facenet.get_image_paths_and_labels(dataset)
        print('Number of images: %d' % len(paths))

        print('Loading feature extraction model')
        modeldir = './20170512-110547/20170512-110547.pb'
        facenet.load_model(modeldir)

        images_placeholder = tf.get_default_graph().get_tensor_by_name("input:0")
        embeddings = tf.get_default_graph().get_tensor_by_name("embeddings:0")
        phase_train_placeholder = tf.get_default_graph().get_tensor_by_name("phase_train:0")
        embedding_size = embeddings.get_shape()[1]

        # Run forward pass to calculate embeddings
        print('Calculating features for images')
        batch_size = 1000
        image_size = 160
        nrof_images = len(paths)
        nrof_batches_per_epoch = int(math.ceil(1.0 * nrof_images / batch_size))
        emb_array = np.zeros((nrof_images, embedding_size))
        for i in range(nrof_batches_per_epoch):
            start_index = i * batch_size
            end_index = min((i + 1) * batch_size, nrof_images)
            paths_batch = paths[start_index:end_index]
            images = facenet.load_data(paths_batch, False, False, image_size)
            feed_dict = {images_placeholder: images, phase_train_placeholder: False}
            emb_array[start_index:end_index, :] = sess.run(embeddings, feed_dict=feed_dict)

        # �먮챸
        if len(emb_array) == 12:
            emb1= np.mean(emb_array[0:5,:], axis=0)
            emb2= np.mean(emb_array[6:11,:], axis=0)
            emb3= np.vstack([emb1,emb2])
            np.savetxt('emb.out',emb3)

        # �몃챸
        if len(emb_array) == 18:
            emb1= np.mean(emb_array[0:5,:], axis=0)
            emb2= np.mean(emb_array[6:11,:], axis=0)
            emb3= np.mean(emb_array[12:17,:], axis=0)
            emb4= np.vstack([emb1,emb2,emb3])
            np.savetxt('emb.out',emb4)

        # �ㅻ챸
        if len(emb_array) == 24:
            emb1= np.mean(emb_array[0:5,:], axis=0)
            emb2= np.mean(emb_array[6:11,:], axis=0)
            emb3= np.mean(emb_array[12:17,:], axis=0)
            emb4= np.mean(emb_array[18:23,:], axis=0)
            emb5= np.vstack([emb1,emb2,emb3,emb4])
            np.savetxt('emb.out',emb5)

        # �ㅼ꽢紐�
        if len(emb_array) == 30:
            emb1= np.mean(emb_array[0:5,:], axis=0)
            emb2= np.mean(emb_array[6:11,:], axis=0)
            emb3= np.mean(emb_array[12:17,:], axis=0)
            emb4= np.mean(emb_array[18:23,:], axis=0)
            emb5= np.mean(emb_array[24:29,:], axis=0)
            emb6= np.vstack([emb1,emb2,emb3,emb4,emb5])
            np.savetxt('emb.out',emb6)
 
        classifier_filename = './classifier_output/ex.pkl'
        classifier_filename_exp = os.path.expanduser(classifier_filename)

        # Train classifier
        print('Training classifier')
        model = SVC(kernel='linear', probability=True)
        model.fit(emb_array, labels)

        # Create a list of class names
        class_names = [cls.name.replace('_', ' ') for cls in dataset]

        # Saving classifier model
        with open(classifier_filename_exp, 'wb') as outfile:
            pickle.dump((model, class_names), outfile, protocol=2)
        print('Saved classifier model to file "%s"' % classifier_filename_exp)
