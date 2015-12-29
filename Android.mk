LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

# Include res dir from chips
#chips_dir := ../../../frameworks/ex/chips/res
color_picker_dir := ../../../../frameworks/opt/colorpicker/res
datetimepicker_dir := ../../../../frameworks/opt/datetimepicker/res
timezonepicker_dir := ../../../../frameworks/opt/timezonepicker/res
res_dirs := $(chips_dir) $(color_picker_dir) $(datetimepicker_dir) $(timezonepicker_dir) res
src_dirs := src

#LOCAL_EMMA_COVERAGE_FILTER := +com.android.calendar.*

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under,$(src_dirs))

# unbundled
LOCAL_STATIC_JAVA_LIBRARIES := \
        colorpicker \
        android-opt-datetimepicker \
        android-opt-timezonepicker \
        umeng-analytics-v5.5.3 \
        android-support-v4


 
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_PACKAGE_NAME := TrafficMonitor

#LOCAL_PROGUARD_FLAG_FILES := proguard.flags \
#                             ../../../frameworks/opt/datetimepicker/proguard.flags

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.android.colorpicker
LOCAL_AAPT_FLAGS += --extra-packages com.android.datetimepicker
LOCAL_AAPT_FLAGS += --extra-packages com.android.timezonepicker

#LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

############################################################### 
include $(CLEAR_VARS) 
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += umeng-analytics-v5.5.3:libs/umeng-analytics-v5.5.3.jar                        # 引用名：jar包名 
include $(BUILD_MULTI_PREBUILT) 
################################################################ 
