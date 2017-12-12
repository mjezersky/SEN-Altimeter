package com.sen.altimeter;


public class Altimeter {

    public static final String WEATHER_API_KEY = "a43bc05af14ac203082821dee6d939be";
    public static String latitude = "37.8267";
    public static String longtitude = "-122.4233";


    public static double calculate(double sensorPressure, double seaLevelPressure, double tempCelsius) {

        return (( Math.pow(seaLevelPressure/sensorPressure, 1.0/5.257) - 1) * (tempCelsius+273.15) ) / 0.0065;
    }



}