package com.example.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

// --- Firebase Imports ---
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
// --- End Imports ---

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for the "My Meals" tab within AddMealFragment.
 * Lists all meals the user has previously saved from Firebase.
 * Clicking a meal adds it to the shared AddMealViewModel "cart".
 */
public class
MyMealsFragment extends Fragment {

    private static final String TAG = "MyMealsFragment";
    private RecyclerView rvMyMealsList;
    private EditText etSearchMyMeals;
    private SavedMealAdapter adapter;
    private AddMealViewModel addMealViewModel; // The shared "cart" ViewModel

    // --- Firebase ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<SavedMeal> allSavedMeals = new ArrayList<>(); // For search filtering

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the Activity-scoped "cart" ViewModel
        FragmentActivity activity = requireActivity();
        addMealViewModel = new ViewModelProvider(activity).get(AddMealViewModel.class);

        // --- Initialize Firebase ---
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_meals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvMyMealsList = view.findViewById(R.id.rvMyMealsList);
        etSearchMyMeals = view.findViewById(R.id.etSearchMyMeals);

        // --- Setup Adapter (empty at first) ---
        adapter = new SavedMealAdapter(new ArrayList<>(), savedMeal -> {
            // *** This is the core logic for this fragment ***
            // When a saved meal is clicked, add all its items to the "cart"
            if (savedMeal.getFoodEntries() != null) {
                for (FoodEntry entry : savedMeal.getFoodEntries()) {
                    addMealViewModel.addFoodToMeal(entry.getFood(), entry.getGrams());
                }
                Toast.makeText(getContext(), "Added '" + savedMeal.getName() + "' to cart", Toast.LENGTH_SHORT).show();
            }
        });
        rvMyMealsList.setAdapter(adapter);

        // --- Load saved meals from Firebase ---
        loadSavedMeals();

        // --- Setup search filter ---
        etSearchMyMeals.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // The adapter handles the filtering logic
                adapter.filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the list every time the tab becomes visible
        // This ensures new saved meals (from btnSaveMeal) appear
        loadSavedMeals();
    }

    /**
     * Loads all SavedMeal objects from the user's "saved_meals" collection.
     */
    private void loadSavedMeals() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "No user logged in to load saved meals.");
            allSavedMeals.clear();
            adapter.updateMeals(allSavedMeals);
            return;
        }
        String uid = currentUser.getUid();

        // Path: /users/{uid}/saved_meals
        db.collection("users").document(uid)
                .collection("saved_meals")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No saved meals found.");
                        allSavedMeals.clear();
                        adapter.updateMeals(allSavedMeals);
                        // You could show an "empty" message here
                        return;
                    }

                    allSavedMeals.clear(); // Clear placeholder/old data
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Convert document to our SavedMeal POJO
                        SavedMeal meal = document.toObject(SavedMeal.class);

                        // Set the document ID for potential future use (e.g., delete)
                        meal.setDocumentId(document.getId());

                        // *** THIS IS THE FIX ***
                        // Totals are not saved in the DB, so we must
                        // calculate them here before displaying.
                        // This now works because calculateTotals() has no arguments.
                        meal.calculateTotals();

                        allSavedMeals.add(meal);
                    }

                    Log.d(TAG, "Loaded " + allSavedMeals.size() + " saved meals.");
                    adapter.updateMeals(allSavedMeals); // Update the adapter
                    // Keep the current search text to maintain filter after resuming
                    // etSearchMyMeals.setText("");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading saved meals", e);
                    Toast.makeText(getContext(), "Error loading saved meals.", Toast.LENGTH_SHORT).show();
                });
    }
}


//Used Gemini AI for Genarations and Error Handlings
