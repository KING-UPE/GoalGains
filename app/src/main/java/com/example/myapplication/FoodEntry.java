package com.example.myapplication;

import com.google.firebase.firestore.Exclude;

/**
 * Represents a specific instance of a Food with a set weight.
 */
public class FoodEntry {
    private Food food;
    private int grams;

    @Exclude
    private String documentId;

    public FoodEntry(Food food, int grams, String documentId) {
        this.food = food;
        this.grams = grams;
        this.documentId = documentId;
    }

    public FoodEntry(Food food, int grams) {
        this.food = food;
        this.grams = grams;
    }

    public FoodEntry() {}

    // ============================================================
    // Firestore fields
    // ============================================================
    public Food getFood() {
        return food;
    }

    public int getGrams() {
        return grams;
    }

    public void setFood(Food food) {
        this.food = food;
    }

    public void setGrams(int grams) {
        this.grams = grams;
    }

    // ============================================================
    // Non-Firestore fields
    // ============================================================
    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    @Exclude
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    // ============================================================
    // Calculated macros (updated to match Food.java method names)
    // ============================================================

    @Exclude
    private double getMultiplier() {
        return grams / 100.0;
    }

    @Exclude
    public int getCalculatedCalories() {
        if (food == null) return 0;

        double carbsPer100g = food.carbsPer100();
        double fibersPer100g = food.fibersPer100();
        double proteinPer100g = food.proteinPer100();
        double fatPer100g = food.fatPer100();

        double netCarbsPer100g = carbsPer100g - fibersPer100g;
        if (netCarbsPer100g < 0) netCarbsPer100g = 0;

        double caloriesPer100g =
                (netCarbsPer100g * 4) +
                        (proteinPer100g * 4) +
                        (fatPer100g * 9);

        return (int) (caloriesPer100g * getMultiplier());
    }

    @Exclude
    public double getCalculatedCarbs() {
        if (food == null) return 0.0;
        return food.carbsPer100() * getMultiplier();
    }

    @Exclude
    public double getCalculatedProtein() {
        if (food == null) return 0.0;
        return food.proteinPer100() * getMultiplier();
    }

    @Exclude
    public double getCalculatedFat() {
        if (food == null) return 0.0;
        return food.fatPer100() * getMultiplier();
    }

    @Exclude
    public double getCalculatedFibers() {
        if (food == null) return 0.0;
        return food.fibersPer100() * getMultiplier();
    }
}


//Used Gemini AI for Genarations and Error Handlings
