package com.example.myapplication;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;

/**
 * POJO that is SAVED to the daily_logs collection in Firestore.
 * It's a lightweight object containing the full Food data and the grams.
 */
public class LoggedFoodItem implements Serializable {

    private Food food; // Embed the entire Food object
    private int grams;

    @Exclude
    private String documentId; // This will hold the Firestore document ID of THIS item

    // --- CONSTRUCTORS ---

    // Default constructor is required for Firestore
    public LoggedFoodItem() {}

    public LoggedFoodItem(Food food, int grams) {
        this.food = food;
        this.grams = grams;
    }

    // --- GETTERS AND SETTERS ---

    public Food getFood() { return food; }
    public void setFood(Food food) { this.food = food; }

    public int getGrams() { return grams; }
    public void setGrams(int grams) { this.grams = grams; }

    @Exclude
    public String getDocumentId() { return documentId; }
    @Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    // --- CONVERSION METHOD ---

    /**
     * Converts this database object into a UI-friendly FoodEntry object.
     * It correctly passes the document ID needed for deletion.
     */
    @Exclude
    public FoodEntry toFoodEntry() {
        // This is the constructor we added: Food, int, String
        return new FoodEntry(this.food, this.grams, this.documentId);
    }
}


//Used Gemini AI for Genarations and Error Handlings
