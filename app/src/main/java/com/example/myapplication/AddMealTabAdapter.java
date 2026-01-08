package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * This adapter provides the 3 fragments for the AddMealFragment's ViewPager.
 */
public class AddMealTabAdapter extends FragmentStateAdapter {

    public AddMealTabAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                // Tab 1: Search for individual foods
                return new AddMealSearchFragment();
            case 1:
                // Tab 2: List of user's saved meals
                return new MyMealsFragment();
            case 2:
                // Tab 3: The "cart" of foods being added
                return new CreateMealFragment();
            default:
                return new Fragment(); // Should not happen
        }
    }

    @Override
    public int getItemCount() {
        return 3; // We have 3 tabs
    }
}
//Used Gemini AI for Genarations and Error Handlings
