package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The main dashboard fragment for displaying logged meals.
 * Implements {@link LoggedFoodAdapter.OnLoggedFoodItemClickListener} to handle delete events.
 */
public class MealsFragment extends Fragment implements LoggedFoodAdapter.OnLoggedFoodItemClickListener {

    private static final String TAG = "MealsFragment";
    private TabLayout tabLayoutMeals;
    private RecyclerView rvLoggedFoodsList;
    private LoggedFoodAdapter loggedFoodAdapter;
    private TextView tvEmptyList;

    // --- Calendar Views ---
    private TextView tvTitle;
    private TextView tvWeekRange;
    private ImageView ivCalendarPrev;
    private ImageView ivCalendarNext;
    private RecyclerView rvCalendarDays;

    // --- Logic Vars ---
    private CalendarDayAdapter calendarAdapter;
    private LocalDate selectedDate;
    private final DateTimeFormatter weekRangeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.US);
    private final DateTimeFormatter dateStringFormatter = DateTimeFormatter.ISO_LOCAL_DATE; // e.g., "2025-11-10"

    // --- Firebase & ViewModel ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private MealsViewModel mealsViewModel; // Activity-scoped ViewModel for date/meal
    private FirebaseUser currentUser;
    private String uid; // Current user's ID

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Initialize Firebase ---
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            uid = currentUser.getUid();
        }

        // Get the Activity-scoped ViewModel. This is shared with MainActivity
        // so the FAB knows which date and meal to add to.
        FragmentActivity activity = requireActivity();
        mealsViewModel = new ViewModelProvider(activity).get(MealsViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- Find all views ---
        tabLayoutMeals = view.findViewById(R.id.tabLayoutMeals);
        rvLoggedFoodsList = view.findViewById(R.id.rvLoggedFoodsList);
        tvEmptyList = view.findViewById(R.id.tvEmptyList);

        // --- Find Calendar views ---
        tvTitle = view.findViewById(R.id.tvTitle);
        tvWeekRange = view.findViewById(R.id.tvWeekRange);
        ivCalendarPrev = view.findViewById(R.id.ivCalendarPrev);
        ivCalendarNext = view.findViewById(R.id.ivCalendarNext);
        rvCalendarDays = view.findViewById(R.id.rvCalendarDays);

        // --- Set up Calendar ---
        selectedDate = LocalDate.now(); // Default to today
        calendarAdapter = new CalendarDayAdapter(new ArrayList<>(), selectedDate, (clickedDate) -> {
            // Lambda for handling date clicks
            updateSelectedDate(clickedDate);
        });
        rvCalendarDays.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCalendarDays.setAdapter(calendarAdapter);
        updateCalendarWeek(selectedDate); // Load the current week

        // --- Calendar Navigation ---
        ivCalendarPrev.setOnClickListener(v -> {
            LocalDate newWeekDate = selectedDate.minusWeeks(1);
            updateCalendarWeek(newWeekDate);
            updateSelectedDate(newWeekDate); // Select same day in the new week
        });
        ivCalendarNext.setOnClickListener(v -> {
            LocalDate newWeekDate = selectedDate.plusWeeks(1);
            updateCalendarWeek(newWeekDate);
            updateSelectedDate(newWeekDate); // Select same day in the new week
        });

        // --- Set up Logged Foods RecyclerView ---
        setupLoggedFoodsList();

        // --- Set up Meal Tab logic ---
        // *** THIS IS THE UPDATED SECTION ***
        // Add the tabs manually
        tabLayoutMeals.addTab(tabLayoutMeals.newTab().setText("Breakfast"));
        tabLayoutMeals.addTab(tabLayoutMeals.newTab().setText("Lunch"));
        tabLayoutMeals.addTab(tabLayoutMeals.newTab().setText("Dinner"));
        tabLayoutMeals.addTab(tabLayoutMeals.newTab().setText("Snacks"));
        // *** END OF UPDATE ***

        tabLayoutMeals.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String mealType = tab.getText().toString();
                mealsViewModel.setSelectedMealType(mealType); // Update Activity ViewModel
                loadMealsForDateAndType(selectedDate, mealType); // Reload data
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // --- Initial Load ---
        updateSelectedDate(selectedDate); // Sets title and updates ViewModel

        // Select the first tab and update ViewModel
        // This code now works because the tabs were added above
        TabLayout.Tab firstTab = tabLayoutMeals.getTabAt(0);
        if (firstTab != null) {
            firstTab.select();
            mealsViewModel.setSelectedMealType(firstTab.getText().toString());
        }

        // Initial data load for "Today" and "Breakfast"
        loadMealsForDateAndType(selectedDate, mealsViewModel.selectedMealType.getValue());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to the fragment (e.g., after adding a meal)
        if (selectedDate != null && mealsViewModel.selectedMealType.getValue() != null) {
            // Update user in case they logged out and logged in as someone else
            currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                uid = currentUser.getUid();
            } else {
                uid = null;
            }
            // Reload the data from Firestore
            loadMealsForDateAndType(selectedDate, mealsViewModel.selectedMealType.getValue());
        }
    }

    /**
     * Initializes the RecyclerView for logged foods with its adapter.
     * Passes 'this' fragment as the click listener for the adapter.
     */
    private void setupLoggedFoodsList() {
        // Pass 'this' as the OnLoggedFoodItemClickListener
        loggedFoodAdapter = new LoggedFoodAdapter(new ArrayList<>(), this);
        rvLoggedFoodsList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvLoggedFoodsList.setAdapter(loggedFoodAdapter);
    }

    /**
     * This is a key logic function. Called when a new date is selected from the calendar.
     */
    private void updateSelectedDate(LocalDate newDate) {
        this.selectedDate = newDate;

        // --- UPDATE VIEWMODEL ---
        // This informs the MainActivity (and FAB) of the new date
        mealsViewModel.setSelectedDate(newDate);

        // 1. Update the main title (e.g., "Today", "Yesterday")
        if (newDate.equals(LocalDate.now())) {
            tvTitle.setText("Today's Meals");
        } else if (newDate.equals(LocalDate.now().plusDays(1))) {
            tvTitle.setText("Tomorrow's Meals");
        } else if (newDate.equals(LocalDate.now().minusDays(1))) {
            tvTitle.setText("Yesterday's Meals");
        } else {
            // Format for other dates, e.g., "Sun, Oct 20"
            tvTitle.setText(newDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)));
        }

        // 2. Tell the calendar adapter to update its selected item UI
        calendarAdapter.setSelectedDate(newDate);

        // 3. Reload the meal list for the newly selected date
        if (tabLayoutMeals != null && tabLayoutMeals.getSelectedTabPosition() != -1) {
            String currentMealType = tabLayoutMeals.getTabAt(tabLayoutMeals.getSelectedTabPosition()).getText().toString();
            loadMealsForDateAndType(selectedDate, currentMealType);
        }
    }

    /**
     * This function loads the meal list from Firestore.
     * This is the "READ" part of the database connection.
     */
    private void loadMealsForDateAndType(LocalDate date, String mealType) {
        if (currentUser == null || uid == null) {
            // No user logged in, just show an empty list
            loggedFoodAdapter.setFoodEntries(new ArrayList<>());
            updateEmptyState(true);
            return;
        }

        // Format date to "yyyy-MM-dd" to match what we save in Firestore
        String dateString = date.format(dateStringFormatter);

        Log.d(TAG, "Loading meals for: " + uid + " on " + dateString + " for " + mealType);

        // This is the core database query:
        // /users/{uid}/daily_logs/{dateString}/{mealType}
        db.collection("users")
                .document(uid)
                .collection("daily_logs")
                .document(dateString)
                .collection(mealType) // e.g., "Breakfast"
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<FoodEntry> meals = new ArrayList<>();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No documents found for " + mealType);
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                // Convert the Firestore document to our LoggedFoodItem POJO
                                LoggedFoodItem loggedItem = document.toObject(LoggedFoodItem.class);

                                // *** CRITICAL STEP ***
                                // Store the Firestore document ID inside the POJO.
                                // We need this ID to delete the item later.
                                loggedItem.setDocumentId(document.getId());

                                // Convert the POJO to a FoodEntry for the adapter/UI
                                meals.add(loggedItem.toFoodEntry());
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing logged item: " + document.getId(), e);
                                // This item is broken, skip it
                            }
                        }
                        Log.d(TAG, "Loaded " + meals.size() + " items for " + mealType);
                    }

                    // Update the adapter with the new list
                    loggedFoodAdapter.setFoodEntries(meals);
                    // Show/Hide the "No foods logged yet" message
                    updateEmptyState(meals.isEmpty());

                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error loading meals for " + mealType, e);
                    Toast.makeText(getContext(), "Error loading meals.", Toast.LENGTH_SHORT).show();
                    updateEmptyState(true);
                });
    }

    /**
     * Helper method to show/hide the "empty" message.
     */
    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            rvLoggedFoodsList.setVisibility(View.GONE);
            tvEmptyList.setVisibility(View.VISIBLE);
        } else {
            rvLoggedFoodsList.setVisibility(View.VISIBLE);
            tvEmptyList.setVisibility(View.GONE);
        }
    }

    /**
     * This function updates the calendar adapter with a new 7-day week.
     */
    private void updateCalendarWeek(LocalDate dateInWeek) {
        List<LocalDate> days = new ArrayList<>();

        // Find the first day of the week (Monday)
        LocalDate startOfWeek = dateInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        // Find the last day of the week (Sunday)
        LocalDate endOfWeek = dateInWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // Add all 7 days to the list
        LocalDate currentDate = startOfWeek;
        while (!currentDate.isAfter(endOfWeek)) {
            days.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }

        // Update the adapter's list of days
        calendarAdapter.setDays(days);

        // Update the week range text (e.g., "10 Nov - 16 Nov")
        String weekRange = startOfWeek.format(weekRangeFormatter) + " - " + endOfWeek.format(weekRangeFormatter);
        tvWeekRange.setText(weekRange);
    }

    /**
     * This is the implementation of the click listener from LoggedFoodAdapter.
     * It handles the deletion of a logged food item.
     */
    @Override
    public void onDeleteClick(FoodEntry entry) {
        if (currentUser == null || uid == null || entry.getDocumentId() == null) {
            Toast.makeText(getContext(), "Error: Could not delete item.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Safety check for broken entry
        if (entry.getFood() == null) {
            Toast.makeText(getContext(), "Cannot delete invalid item.", Toast.LENGTH_SHORT).show();
            return;
        }

        String mealType = mealsViewModel.selectedMealType.getValue();
        String dateString = mealsViewModel.selectedDate.getValue().format(dateStringFormatter);

        if (mealType == null || dateString == null) {
            Toast.makeText(getContext(), "Error: Date or meal type not selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show a confirmation dialog before deleting
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete '" + entry.getFood().getName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // User clicked "Delete"
                    performDelete(entry, uid, dateString, mealType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Performs the actual Firestore deletion using a WriteBatch
     * to ensure data consistency (atomicity).
     */
    private void performDelete(FoodEntry entry, String uid, String dateString, String mealType) {
        // 1. Get reference to the food item to delete
        // e.g., /users/{uid}/daily_logs/{date}/Breakfast/{docId}
        DocumentReference itemDocRef = db.collection("users")
                .document(uid)
                .collection("daily_logs")
                .document(dateString)
                .collection(mealType)
                .document(entry.getDocumentId());

        // 2. Get reference to the daily log document (which stores the totals)
        // e.g., /users/{uid}/daily_logs/{date}
        DocumentReference dateDocRef = db.collection("users")
                .document(uid)
                .collection("daily_logs")
                .document(dateString);

        // 3. Get the *negative* nutrition values to "increment" by (which subtracts)
        double caloriesToDecrement = -entry.getCalculatedCalories();
        double carbsToDecrement = -entry.getCalculatedCarbs();
        double proteinToDecrement = -entry.getCalculatedProtein();
        double fatToDecrement = -entry.getCalculatedFat();

        // 4. Create a new WriteBatch
        WriteBatch batch = db.batch();

        // 5. Add the delete operation to the batch
        batch.delete(itemDocRef);

        // 6. Add the update operation for totals to the batch
        Map<String, Object> dailyTotals = new HashMap<>();
        dailyTotals.put("totalCalories", FieldValue.increment(caloriesToDecrement));
        dailyTotals.put("totalCarbs", FieldValue.increment(carbsToDecrement));
        dailyTotals.put("totalProtein", FieldValue.increment(proteinToDecrement));
        dailyTotals.put("totalFat", FieldValue.increment(fatToDecrement));

        // We use SetOptions.merge() to safely update totals without overwriting other fields
        batch.set(dateDocRef, dailyTotals, SetOptions.merge());

        // 7. Commit the batch (atomically performs both operations)
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully deleted item and updated totals.");
                    Toast.makeText(getContext(), "Item deleted", Toast.LENGTH_SHORT).show();
                    // 8. Refresh the list to show the change
                    loadMealsForDateAndType(selectedDate, mealType);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error writing delete batch", e);
                    Toast.makeText(getContext(), "Error deleting item. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }
}

//Used Gemini AI for Genarations and Error Handlings
