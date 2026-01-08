package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    private List<Food> fullList;      // all foods
    private List<Food> filteredList;  // filtered results
    private final OnFoodItemClickListener listener;

    public interface OnFoodItemClickListener {
        void onFoodItemClick(Food food);
    }

    public FoodAdapter(List<Food> foods, OnFoodItemClickListener listener) {
        this.fullList = new ArrayList<>(foods != null ? foods : new ArrayList<>());
        this.filteredList = new ArrayList<>(this.fullList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_food, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        Food food = filteredList.get(position);
        holder.bind(food, listener);
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    // stable filter
    public void filter(String query) {
        String q = query == null ? "" : query.toLowerCase().trim();
        filteredList.clear();

        if (q.isEmpty()) {
            filteredList.addAll(fullList);
        } else {
            for (Food f : fullList) {
                String name = f.getName() == null ? "" : f.getName().toLowerCase();
                String cat = f.getMainCategory() == null ? "" : f.getMainCategory().toLowerCase();
                if (name.contains(q) || cat.contains(q)) {
                    filteredList.add(f);
                }
            }
        }
        notifyDataSetChanged();
    }

    // replace dataset safely
    public void updateData(List<Food> newFoods) {
        this.fullList = new ArrayList<>(newFoods != null ? newFoods : new ArrayList<>());
        this.filteredList = new ArrayList<>(this.fullList);
        notifyDataSetChanged();
    }

    static class FoodViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoodImage;
        TextView tvFoodName;
        TextView tvFoodCategory;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoodImage = itemView.findViewById(R.id.ivFoodImage);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvFoodCategory = itemView.findViewById(R.id.tvFoodCategory);
        }

        public void bind(final Food food, final OnFoodItemClickListener listener) {
            tvFoodName.setText(food.getName() != null ? food.getName() : "Unnamed");
            tvFoodCategory.setText(food.getMainCategory() != null ? food.getMainCategory() : "");

            String url = food.getImageUrl();
            if (url != null && !url.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(url)
                        .placeholder(R.drawable.ic_food_placeholder)
                        .error(R.drawable.ic_food_placeholder)
                        .circleCrop()
                        .into(ivFoodImage);
            } else {
                Glide.with(itemView.getContext())
                        .load(R.drawable.ic_food_placeholder)
                        .circleCrop()
                        .into(ivFoodImage);
            }

            itemView.setOnClickListener(v -> listener.onFoodItemClick(food));
        }
    }
}


//Used Gemini AI for Genarations and Error Handlings
