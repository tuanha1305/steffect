cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11 -fPIC  -shared -Wno-error, -Wno-format-security")

add_library(stmobile_jni SHARED
       stmobile_authentification_jni.cpp
       stmobile_beautify_jni.cpp
       stmobile_face_attribute_jni.cpp
       stmobile_filter_jni.cpp
       stmobile_humanaction_jni.cpp
       stmobile_object_jni.cpp
       stmobile_sticker_jni.cpp
       stmobile_streamfilter_jni.cpp
        utils.cpp)

target_include_directories(stmobile_jni PRIVATE
                          ${CMAKE_CURRENT_SOURCE_DIR}
                          ${CMAKE_CURRENT_SOURCE_DIR}/prebuilt/include)

add_library(lib_mobile SHARED IMPORTED )
set_target_properties(lib_mobile PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libst_mobile.so)

# Include libraries needed for hello-jni lib
target_link_libraries(stmobile_jni
                      android
                      log
                      lib_mobile
                      jnigraphics
                      GLESv2)
