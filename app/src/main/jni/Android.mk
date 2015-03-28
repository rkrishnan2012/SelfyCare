LOCAL_PATH := $(call my-dir)
EMOTIME_INCLUDES:=$(LOCAL_PATH)/include
include $(CLEAR_VARS)

OPENCV_CAMERA_MODULES:=on
OPENCV_INSTALL_MODULES:=on
#OPENCV_LIB_TYPE:=SHARED
OPENCV_LIB_TYPE:=STATIC
include ../../sdk/native/jni/OpenCV.mk

LOCAL_SRC_FILES  += EmotionClassifier.cpp
LOCAL_SRC_FILES  += EmotionClassifier_jni.cpp


FILE_LIST := $(wildcard $(LOCAL_PATH)/detector/*.cpp)
LOCAL_SRC_FILES += $(FILE_LIST:$(LOCAL_PATH)/%=%)

FILE_LIST := $(wildcard $(LOCAL_PATH)/training/*.cpp)
LOCAL_SRC_FILES += $(FILE_LIST:$(LOCAL_PATH)/%=%)

FILE_LIST := $(wildcard $(LOCAL_PATH)/utils/*.cpp)
LOCAL_SRC_FILES += $(FILE_LIST:$(LOCAL_PATH)/%=%)

FILE_LIST := $(wildcard $(LOCAL_PATH)/facedetector/*.cpp)
LOCAL_SRC_FILES += $(FILE_LIST:$(LOCAL_PATH)/%=%)

FILE_LIST := $(wildcard $(LOCAL_PATH)/gaborbank/*.cpp)
LOCAL_SRC_FILES += $(FILE_LIST:$(LOCAL_PATH)/%=%)

LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_C_INCLUDES += $(LOCAL_PATH)/facedetector
LOCAL_C_INCLUDES += $(LOCAL_PATH)/gaborbank
LOCAL_C_INCLUDES += $(LOCAL_PATH)/detector
LOCAL_C_INCLUDES += $(LOCAL_PATH)/training
LOCAL_C_INCLUDES += $(LOCAL_PATH)/utils
LOCAL_C_INCLUDES += $(EMOTIME_INCLUDES)

LOCAL_LDLIBS     += -llog -ldl

LOCAL_MODULE     := detection_based_tracker

include $(BUILD_SHARED_LIBRARY)


