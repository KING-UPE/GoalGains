package com.example.myapplication;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A shared ViewModel for the "Add Meal" flow.
 * This ViewModel is scoped to the NavController of the AddMealFragment.
 * It is shared between AddMealFragment and its three child fragments
 * (AddMealSearchFragment, MyMealsFragment, CreateMealFragment).
 *
 * It holds the "cart" (the list of foods being added) and calculates totals.
 */
public class AddMealViewModel extends ViewModel {

    // --- The "Cart" ---
    // Holds the list of FoodEntry items currently being added.
    private final MutableLiveData<List<FoodEntry>> _currentMealEntries = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<FoodEntry>> currentMealEntries = _currentMealEntries;

    // --- LiveData for Calculated Totals ---
    // These are observed by AddMealFragment to show totals in the bottom bar.
    private final MutableLiveData<Integer> _totalCalories = new MutableLiveData<>(0);
    public final LiveData<Integer> totalCalories = _totalCalories;

    private final MutableLiveData<Double> _totalCarbs = new MutableLiveData<>(0.0);
    public final LiveData<Double> totalCarbs = _totalCarbs;

    private final MutableLiveData<Double> _totalProtein = new MutableLiveData<>(0.0);
    public final LiveData<Double> totalProtein = _totalProtein;

    private final MutableLiveData<Double> _totalFat = new MutableLiveData<>(0.0);
    public final LiveData<Double> totalFat = _totalFat;

    /**
     * Adds a food to the "cart" and recalculates totals.
     * Called from AddMealSearchFragment and MyMealsFragment.
     */
    public void addFoodToMeal(Food food, int grams) {
        FoodEntry newEntry = new FoodEntry(food, grams);

        // Get the current list, add the new item
        List<FoodEntry> currentList = _currentMealEntries.getValue();
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        currentList.add(newEntry);
        _currentMealEntries.setValue(currentList); // Triggers observers

        // Recalculate all totals
        recalculateTotals();
    }

    /**
     * Removes a food from the "cart" and recalculates totals.
     * Called from CreateMealFragment.
     */
    public void removeFoodFromMeal(FoodEntry entry) {
        List<FoodEntry> currentList = _currentMealEntries.getValue();
        if (currentList != null) {
            currentList.remove(entry);
            _currentMealEntries.setValue(currentList); // Triggers observers
        }
        recalculateTotals();
    }

    /**
     * Recalculates all nutrition based on the current list of FoodEntry items.
     * This is private and called internally whenever the list changes.
     */
    private void recalculateTotals() {
        List<FoodEntry> currentList = _currentMealEntries.getValue();
        if (currentList == null) return;

        int calories = 0;
        double carbs = 0.0;
        double protein = 0.0;
        double fat = 0.0;

        // --- THIS IS THE FIX ---
        // Use the new method names from FoodEntry.java
        for (FoodEntry entry : currentList) {
            calories += entry.getCalculatedCalories();     // <- Corrected
            carbs += entry.getCalculatedCarbs();           // <- Corrected
            protein += entry.getCalculatedProtein();       // <- Corrected
            fat += entry.getCalculatedFat();               // <- Corrected
        }
        // --- END OF FIX ---

        // Set the LiveData values, which triggers observers in AddMealFragment
        _totalCalories.setValue(calories);
        _totalCarbs.setValue(carbs);
        _totalProtein.setValue(protein);
        _totalFat.setValue(fat);
    }

    /**
     * Clears the cart.
     * Called from AddMealFragment after successfully adding to the log.
     */
    public void clearCurrentMeal() {
        _currentMealEntries.setValue(new ArrayList<>());
        recalculateTotals(); // Resets totals to 0
    }

    /**
     * Helper to format doubles for the UI.
     */
    public String formatDouble(double value) {
        return String.format(Locale.US, "%.1fg", value);
    }
}

//Used Gemini AI for Genarations and Error Handlings
