# HeartRateSensor

## Description
This application simulates the operation of a heart rate measuring device and blood oxygen saturation. The repository contains a part related to the operation of the MAX30100 sensor and a part related to an application for an Android phone.

## Authors and their responsibilities in the project:
Justyna Malysa
 - creation of an application in C language for STM32 microcontroller handling the MAX30100 sensor
 - creation of a driver for MAX30100 sensor
 - creation of algorithms that calculate the value of heart rate and SpO2 from raw data

Radoslaw Musiał
- development of an application for measuring heart rate and blood saturation
- development of the concept of the chart on which the data was saved
- developing a method for saving data on a graph

Maciej Sudoł
 - development and creation of Android application - Bluetooth connection procedure implementation
 - Estabilishing BT connection between ESP32 and phone (make ESP behave like HC-05 module)
 - dataframe decoding and data aqusition algorithm in android application
 
