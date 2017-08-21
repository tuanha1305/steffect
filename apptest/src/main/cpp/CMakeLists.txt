cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11 -fPIC  -shared -Wno-error, -Wno-format-security")

add_library(beautysdk SHARED
        gl_render.cpp)

# Include libraries needed for hello-jni lib
target_link_libraries(beautysdk
                      android
                      log
                      GLESv2)
