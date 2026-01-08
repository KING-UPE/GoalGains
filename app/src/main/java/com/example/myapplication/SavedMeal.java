package com.example.myapplication;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.util.List;

/**
 * POJO for a user's saved meal template.
 * Stored in the "saved_meals" collection.
 * This version has the corrected calculateTotals() method.
 */
public class SavedMeal {

    private String name;
    private List<FoodEntry> foodEntries;

    @Exclude
    private String documentId;

    // These fields are calculated locally and NOT saved to Firestore.
    @Exclude
    private double totalCalories;
    @Exclude
    private double totalCarbs;
    @Exclude
    private double totalProtein;
    @Exclude
    private double totalFat;

    // --- CONSTRUCTORS ---
    public SavedMeal() {}

    // --- GETTERS & SETTERS for Firestore properties ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @PropertyName("foodEntries")
    public List<FoodEntry> getFoodEntries() { return foodEntries; }

    @PropertyName("foodEntries")
    public void setFoodEntries(List<FoodEntry> foodEntries) { this.foodEntries = foodEntries; }

    @Exclude
    public String getDocumentId() { return documentId; }

    @Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    // --- GETTERS for calculated properties (used by the UI/Adapter) ---
    @Exclude
    public double getTotalCalories() { return totalCalories; }
    @Exclude
    public double getTotalCarbs() { return totalCarbs; }
    @Exclude
    public double getTotalProtein() { return totalProtein; }
    @Exclude
    public double getTotalFat() { return totalFat; }


    // --- CORRECTED HELPER METHOD ---
    /**
     * Calculates the total nutritional values and stores them in the object's fields.
     * This method no longer requires any arguments.
     */
    @Exclude
    public void calculateTotals() {
        // Reset totals before recalculating
        this.totalCalories = 0;
        this.totalCarbs = 0;
        this.totalProtein = 0;
        this.totalFat = 0;

        if (foodEntries == null || foodEntries.isEmpty()) {
            return; // Nothing to calculate
        }

        // --- THIS IS THE FIX ---
        // Use the new 'getCalculated...' method names
        for (FoodEntry entry : foodEntries) {
            if(entry != null) { // Safety check for null entries
                this.totalCalories += entry.getCalculatedCalories();
                this.totalCarbs += entry.getCalculatedCarbs();
                this.totalProtein += entry.getCalculatedProtein();
                this.totalFat += entry.getCalculatedFat();
            }
        }
        // --- END OF FIX ---
    }
}

//Used Gemini AI for Genarations and Error Handlings
