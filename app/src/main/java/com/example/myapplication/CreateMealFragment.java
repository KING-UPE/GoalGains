package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

/**
 * Fragment for the "Create" tab (the cart).
 * This fragment is very simple: it just observes the
 * shared AddMealViewModel and displays the list of FoodEntry items.
 */
public class CreateMealFragment extends Fragment {

    private RecyclerView rvCreateMealList;
    private TextView tvEmptyCart;
    private CreateMealAdapter adapter;
    private AddMealViewModel addMealViewModel; // The shared "cart" ViewModel

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the Activity-scoped "cart" ViewModel
        FragmentActivity activity = requireActivity();
        addMealViewModel = new ViewModelProvider(activity).get(AddMealViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_meal, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCreateMealList = view.findViewById(R.id.rvCreateMealList);
        tvEmptyCart = view.findViewById(R.id.tvEmptyCart);

        // Setup adapter with remove logic
        adapter = new CreateMealAdapter(new ArrayList<>(), foodEntry -> {
            // When 'x' is clicked, remove the item from the ViewModel
            addMealViewModel.removeFoodFromMeal(foodEntry);
        });
        rvCreateMealList.setAdapter(adapter);

        // *** This is the core logic ***
        // Observe the cart in the ViewModel.
        // This LiveData triggers updates when items are added (from Search/MyMeals)
        // or removed (from this fragment).
        addMealViewModel.currentMealEntries.observe(getViewLifecycleOwner(), entries -> {
            // Update the adapter's list
            adapter.updateList(entries);
            // Show/hide the empty message
            tvEmptyCart.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}

//Used Gemini AI for Genarations and Error Handlings
