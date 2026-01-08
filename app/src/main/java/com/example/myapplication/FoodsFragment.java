package com.example.myapplication;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth; // <-- IMPORT
import com.google.firebase.auth.FirebaseUser; // <-- IMPORT
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections; // <-- IMPORT
import java.util.Comparator; // <-- IMPORT
import java.util.HashSet; // <-- IMPORT
import java.util.List;
import java.util.Set; // <-- IMPORT

public class FoodsFragment extends Fragment {

    private static final String TAG = "FoodsFragment";
    private RecyclerView rvFoodsList;
    private FoodAdapter foodAdapter;
    private List<Food> allFoods;
    private EditText etSearchFoods;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth; // <-- ADD
    private FirebaseUser currentUser; // <-- ADD
    private Set<String> likedFoodIds = new HashSet<>(); // <-- ADD

    private NavController navController;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_foods, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance(); // <-- INITIALIZE
        currentUser = mAuth.getCurrentUser(); // <-- GET CURRENT USER

        // Find views
        rvFoodsList = view.findViewById(R.id.rvFoodsList);
        etSearchFoods = view.findViewById(R.id.etSearchFoods);
        ImageView ivBackButton = view.findViewById(R.id.ivBackButton);

        ivBackButton.setOnClickListener(v -> navController.popBackStack());

        // --- LOAD LIKES FIRST, THEN LOAD FOODS ---
        loadUserLikesAndThenFoods();

        // Setup Search Filtering (unchanged)
        etSearchFoods.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (foodAdapter != null) {
                    foodAdapter.filter(s.toString());
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Auto-focus search bar (unchanged)
        if (getArguments() != null && getArguments().getBoolean("focus_search", false)) {
            // ... (unchanged)
        }
    }

    /**
     * Loads the user's liked food IDs first, then calls loadFoodsFromFirestore.
     */
    private void loadUserLikesAndThenFoods() {
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            List<String> likedList = (List<String>) documentSnapshot.get("likedFoods");
                            if (likedList != null) {
                                likedFoodIds.addAll(likedList);
                                Log.d(TAG, "Loaded " + likedFoodIds.size() + " liked food IDs.");
                            }
                        }
                        // --- NOW LOAD FOODS ---
                        loadFoodsFromFirestore();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error loading user likes, loading foods anyway.", e);
                        // --- LOAD FOODS ANYWAY ---
                        loadFoodsFromFirestore();
                    });
        } else {
            // No user, just load foods
            loadFoodsFromFirestore();
        }
    }


    /**
     * Fetches foods, sorts them by "liked" status, and sets the adapter.
     */
    private void loadFoodsFromFirestore() {
        allFoods = new ArrayList<>();
        // Show a progress bar here

        db.collection("foods")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allFoods.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Food food = document.toObject(Food.class);
                        // --- STORE THE DOCUMENT ID ---
                        food.setDocumentId(document.getId());
                        allFoods.add(food);
                    }

                    // --- SORT THE LIST ---
                    Collections.sort(allFoods, (food1, food2) -> {
                        boolean isLiked1 = likedFoodIds.contains(food1.getDocumentId());
                        boolean isLiked2 = likedFoodIds.contains(food2.getDocumentId());

                        if (isLiked1 && !isLiked2) {
                            return -1; // Liked item comes first
                        } else if (!isLiked1 && isLiked2) {
                            return 1;  // Liked item comes first
                        } else {
                            // Both are liked or both are not, sort alphabetically
                            return food1.getName().compareToIgnoreCase(food2.getName());
                        }
                    });

                    Log.d(TAG, "Loaded and sorted " + allFoods.size() + " foods.");

                    // --- SETUP ADAPTER ---
                    foodAdapter = new FoodAdapter(allFoods, food -> {
                        // Pass the Food object (which now has the ID)
                        Bundle args = new Bundle();
                        args.putSerializable("food_item", food);
                        navController.navigate(R.id.action_foodsFragment_to_foodDetailFragment, args);
                    });
                    rvFoodsList.setAdapter(foodAdapter);
                    // Hide progress bar

                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading foods", e);
                    Toast.makeText(getContext(), "Error loading food data.", Toast.LENGTH_SHORT).show();
                });
    }
}

//Used Gemini AI for Genarations and Error Handlings
