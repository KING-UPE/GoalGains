package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for the RecyclerView in MealsFragment.
 * Displays the list of logged foods for a specific day and meal.
 */
public class LoggedFoodAdapter extends RecyclerView.Adapter<LoggedFoodAdapter.LoggedFoodViewHolder> {

    private List<FoodEntry> foodEntries;
    private final OnLoggedFoodItemClickListener listener;

    /**
     * Interface for handling delete clicks.
     * The MealsFragment implements this.
     */
    public interface OnLoggedFoodItemClickListener {
        void onDeleteClick(FoodEntry entry);
    }

    public LoggedFoodAdapter(List<FoodEntry> foodEntries, OnLoggedFoodItemClickListener listener) {
        this.foodEntries = foodEntries;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LoggedFoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_logged_food, parent, false);
        return new LoggedFoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LoggedFoodViewHolder holder, int position) {
        FoodEntry entry = foodEntries.get(position);
        holder.bind(entry, listener); // Pass listener to bind
    }

    @Override
    public int getItemCount() {
        return foodEntries.size();
    }

    /**
     * Updates the list of entries and refreshes the RecyclerView.
     */
    public void setFoodEntries(List<FoodEntry> newEntries) {
        this.foodEntries = newEntries;
        notifyDataSetChanged(); // In a real app, use DiffUtil for better performance
    }

    // --- ViewHolder Class ---
    static class LoggedFoodViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoodImage;
        TextView tvFoodName;
        TextView tvFoodWeight;
        TextView tvFoodCalories;
        ImageView ivDeleteFood; // The delete button

        public LoggedFoodViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoodImage = itemView.findViewById(R.id.ivFoodImage);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvFoodWeight = itemView.findViewById(R.id.tvFoodWeight);
            tvFoodCalories = itemView.findViewById(R.id.tvFoodCalories);
            ivDeleteFood = itemView.findViewById(R.id.ivDeleteFood);
        }

        /**
         * Binds a FoodEntry to the layout.
         */
        public void bind(final FoodEntry entry, final OnLoggedFoodItemClickListener listener) {
            // --- Safety check for broken data from Firestore ---
            if (entry.getFood() == null) {
                tvFoodName.setText("Invalid food data");
                tvFoodWeight.setText("0g");
                tvFoodCalories.setText("0 kcal");
                // You might want to hide the delete button too
                // ivDeleteFood.setVisibility(View.GONE);
                return;
            }
            // --- End safety check ---

            Food food = entry.getFood();
            int grams = entry.getGrams();

            // --- Load Image with Glide ---
            String imageUrl = food.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_food_placeholder)
                        .error(R.drawable.ic_food_placeholder)
                        .into(ivFoodImage);
            } else {
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_food_placeholder)
                        .into(ivFoodImage);
            }
            // --- END Glide logic ---

            tvFoodName.setText(food.getName());
            tvFoodWeight.setText(String.format(Locale.US, "%dg", grams));

            // --- THIS IS THE FIX ---
            // Use the correct method name: getCalculatedCalories()
            tvFoodCalories.setText(String.format(Locale.US, "%d kcal", (int) entry.getCalculatedCalories()));
            // --- END OF FIX ---


            // --- Set click listener for delete ---
            ivDeleteFood.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(entry);
                }
            });
        }
    }
}

//Used Gemini AI for Genarations and Error Handlings
