package com.example.msimangapart3.helperClasses;

import java.util.ArrayList;

public class TempDB {

    private static ArrayList<String> locationId = new ArrayList<>();

    private static String metrics;
    private static String distanceRadius ;
    private static String daysSinceSighting;

    private static ArrayList<String> birdName = new ArrayList<>();
    private static ArrayList<String> birdSpecies = new ArrayList<>();
    private static ArrayList<String> birdlocationName = new ArrayList<>();

    public static ArrayList<String> getBirdName() {
        return birdName;
    }
    public static ArrayList<String> getBirdSpecies() {
        return birdSpecies;
    }
    public static ArrayList<String> getBirdlocationName() {
        return birdlocationName;
    }
    public static ArrayList<String> getLocationId() {
        return locationId;
    }

    public static void addLocationId(String location) {
        locationId.add(location);
    }

    public static String getMetrics() {
        return metrics;
    }

    public static void setMetrics(String metrics) {
        TempDB.metrics = metrics;
    }

    public static String getDistanceRadius() {
        return distanceRadius;
    }

    public static void setDistanceRadius(String distanceRadius) {
        TempDB.distanceRadius = distanceRadius;
    }

    public static String getDaysSinceSighting() {
        return daysSinceSighting;
    }

    public static void setDaysSinceSighting(String daysSinceSighting) {
        TempDB.daysSinceSighting = daysSinceSighting;
    }

    public static void addBirdName(String name){
        birdName.add(name);
    }

    public static void addBirdSpecies(String species){
        birdSpecies.add(species);
    }

    public static ArrayList<String> getBirds(){
        ArrayList<String> birds = new ArrayList<>();
        for (int i = 0; i < birdName.size(); i++) {
            String bird = "bird: " + birdName.get(i) + " species: " + birdSpecies.get(i);
            birds.add(bird);
        }
        return birds;
    }
}
