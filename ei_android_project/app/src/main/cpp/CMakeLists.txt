
cmake_minimum_required(VERSION 3.10.2)
project(EIApp)

add_library(ei SHARED
    native-lib.cpp
)

add_library(tensorflowlite SHARED IMPORTED)
set_target_properties(tensorflowlite PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/tflite/libtensorflowlite.so)

target_include_directories(ei PRIVATE
    ${CMAKE_SOURCE_DIR}/edge-impulse-sdk
    ${CMAKE_SOURCE_DIR}/tflite-model
)

target_link_libraries(ei tensorflowlite log)
