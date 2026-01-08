package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for the "My Meals" list
 */
public class SavedMealAdapter extends RecyclerView.Adapter<SavedMealAdapter.SavedMealViewHolder> {

    private List<SavedMeal> savedMealsFiltered; // The list that is currently displayed
    private final List<SavedMeal> savedMealsFull; // The master list
    private final OnSavedMealClickListener listener;

    /**
     * Interface for clicking on a saved meal.
     * The MyMealsFragment implements this.
     */
    public interface OnSavedMealClickListener {
        void onSavedMealClick(SavedMeal savedMeal);
    }

    public SavedMealAdapter(List<SavedMeal> savedMeals, OnSavedMealClickListener listener) {
        this.savedMealsFiltered = new ArrayList<>(savedMeals);
        this.savedMealsFull = new ArrayList<>(savedMeals);
        this.listener = listener;
    }

    @NonNull
    @Override
    public SavedMealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_saved_meal, parent, false);
        return new SavedMealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedMealViewHolder holder, int position) {
        SavedMeal meal = savedMealsFiltered.get(position);

        // Note: We assume meal.calculateTotals() was called
        // in the Fragment *before* this adapter was updated.

        holder.bind(meal, listener);
    }

    @Override
    public int getItemCount() {
        return savedMealsFiltered.size();
    }

    /**
     * Resets both the master list and filtered list.
     * @param newMeals The new master list from Firebase
     */
    public void updateMeals(List<SavedMeal> newMeals) {
        savedMealsFull.clear();
        savedMealsFull.addAll(newMeals);
        savedMealsFiltered.clear();
        savedMealsFiltered.addAll(newMeals);
        notifyDataSetChanged();
    }

    /**
     * Filters the displayed list based on search text.
     */
    public void filter(String text) {
        savedMealsFiltered.clear();
        if (text.isEmpty()) {
            savedMealsFiltered.addAll(savedMealsFull);
        } else {
            text = text.toLowerCase().trim();
            for (SavedMeal meal : savedMealsFull) {
                // Search by meal name
                if (meal.getName().toLowerCase().contains(text)) {
                    savedMealsFiltered.add(meal);
                }
            }
        }
        notifyDataSetChanged();
    }

    // --- ViewHolder Class ---
    static class SavedMealViewHolder extends RecyclerView.ViewHolder {
        TextView tvSavedMealName, tvMealCarbs, tvMealProtein, tvMealFat, tvMealTotalCalories;

        public SavedMealViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSavedMealName = itemView.findViewById(R.id.tvSavedMealName);
            tvMealCarbs = itemView.findViewById(R.id.tvMealCarbs);
            tvMealProtein = itemView.findViewById(R.id.tvMealProtein);
            tvMealFat = itemView.findViewById(R.id.tvMealFat);
            tvMealTotalCalories = itemView.findViewById(R.id.tvMealTotalCalories);
        }

        public void bind(final SavedMeal meal, final OnSavedMealClickListener listener) {
            // Bind all the data
            tvSavedMealName.setText(meal.getName());
            tvMealTotalCalories.setText(meal.getTotalCalories() + " kcal");

            String carbs = String.format(Locale.US, "%.1fg", meal.getTotalCarbs());
            String protein = String.format(Locale.US, "%.1fg", meal.getTotalProtein());
            String fat = String.format(Locale.US, "%.1fg", meal.getTotalFat());

            tvMealCarbs.setText(carbs);
            tvMealProtein.setText(protein);
            tvMealFat.setText(fat);

            // Set the main item click listener
            itemView.setOnClickListener(v -> listener.onSavedMealClick(meal));
        }
    }
}

//Used Gemini AI for Genarations and Error Handlings
