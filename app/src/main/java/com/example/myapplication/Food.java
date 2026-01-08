package com.example.myapplication;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.List;

/**
 * FIXED VERSION:
 * - No Firestore conflicts
 * - Nested Per100g is now 100% Firestore-safe
 * - Helper macro getters renamed so Firestore will NOT treat them as fields
 */
public class Food implements Serializable {

    @Exclude
    private String documentId;

    // --- Firestore fields ---
    private String name;
    private String imageUrl;
    private double calories;
    private List<String> categories;
    private String description;
    private double rating;
    private Per100g per100g;

    // --- Required empty constructor for Firestore ---
    public Food() {}

    // --- Nested object for macros per 100g (Firestore-safe) ---
    public static class Per100g implements Serializable {
        private double carbs;
        private double fat;
        private double protein;
        private double fibers;

        public Per100g() {}

        public double getCarbs() { return carbs; }
        public double getFat() { return fat; }
        public double getProtein() { return protein; }
        public double getFibers() { return fibers; }

        public void setCarbs(double carbs) { this.carbs = carbs; }
        public void setFat(double fat) { this.fat = fat; }
        public void setProtein(double protein) { this.protein = protein; }
        public void setFibers(double fibers) { this.fibers = fibers; }
    }

    // --- Normal GETTERS (Firestore will use these) ---
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public double getCalories() { return calories; }
    public List<String> getCategories() { return categories; }
    public String getDescription() { return description; }
    public double getRating() { return rating; }
    public Per100g getPer100g() { return per100g; }

    // --- Normal SETTERS (Firestore requires these) ---
    public void setName(String name) { this.name = name; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCalories(double calories) { this.calories = calories; }
    public void setCategories(List<String> categories) { this.categories = categories; }
    public void setDescription(String description) { this.description = description; }
    public void setRating(double rating) { this.rating = rating; }
    public void setPer100g(Per100g per100g) { this.per100g = per100g; }

    // --- ID field (not stored in Firestore) ---
    @Exclude
    public String getDocumentId() { return documentId; }

    @Exclude
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    // ============================================================
    // 🔥 FIXED HELPER METHODS (RENAMED so Firestore will ignore them)
    // ============================================================

    @Exclude
    public String getMainCategory() {
        if (categories != null && !categories.isEmpty()) return categories.get(0);
        return "";
    }

    // IMPORTANT: renamed to avoid Firestore treating as actual getters
    @Exclude
    public double carbsPer100() {
        if (per100g != null) return per100g.getCarbs();
        return 0.0;
    }

    @Exclude
    public double proteinPer100() {
        if (per100g != null) return per100g.getProtein();
        return 0.0;
    }

    @Exclude
    public double fatPer100() {
        if (per100g != null) return per100g.getFat();
        return 0.0;
    }

    @Exclude
    public double fibersPer100() {
        if (per100g != null) return per100g.getFibers();
        return 0.0;
    }
}

//Used Gemini AI for Genarations and Error Handlings