LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := trafficstats
LOCAL_SRC_FILES := com_example_floatwindow_FxService.c
#LOCAL_LDFLAGS :=  -llog 
 
include $(BUILD_SHARED_LIBRARY)
 
