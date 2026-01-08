package com.example.myapplication;

import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.github.mikephil.charting.data.Entry;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgressViewModel extends ViewModel {

    private static final String TAG = "ProgressViewModel";

    private final FirebaseFirestore db;
    private final FirebaseUser currentUser;

    // --- LiveData for User Goals ---
    private final MutableLiveData<Long> _calorieGoal = new MutableLiveData<>(0L);
    public LiveData<Long> getCalorieGoal() { return _calorieGoal; }

    private final MutableLiveData<Long> _carbGoal = new MutableLiveData<>(0L);
    public LiveData<Long> getCarbGoal() { return _carbGoal; }

    private final MutableLiveData<Long> _proteinGoal = new MutableLiveData<>(0L);
    public LiveData<Long> getProteinGoal() { return _proteinGoal; }

    private final MutableLiveData<Long> _fatGoal = new MutableLiveData<>(0L);
    public LiveData<Long> getFatGoal() { return _fatGoal; }

    private final MutableLiveData<String> _userName = new MutableLiveData<>("User");
    public LiveData<String> getUserName() { return _userName; }

    // --- LiveData for Dashboard ---
    private final MutableLiveData<DocumentSnapshot> _todaysLog = new MutableLiveData<>();
    public LiveData<DocumentSnapshot> getTodaysLog() { return _todaysLog; }

    // --- LiveData for Progress Charts ---
    private final MutableLiveData<List<Entry>> _calorieHistory = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Entry>> getCalorieHistory() { return _calorieHistory; }

    private final MutableLiveData<List<Entry>> _carbHistory = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Entry>> getCarbHistory() { return _carbHistory; }

    private final MutableLiveData<List<Entry>> _proteinHistory = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Entry>> getProteinHistory() { return _proteinHistory; }

    private final MutableLiveData<List<Entry>> _fatHistory = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<Entry>> getFatHistory() { return _fatHistory; }

    // --- LiveData for Chart X-Axis Labels ---
    private final MutableLiveData<String[]> _chartLabels = new MutableLiveData<>();
    public LiveData<String[]> getChartLabels() { return _chartLabels; }

    // --- LiveData for the streak count ---
    private final MutableLiveData<Integer> _streakCount = new MutableLiveData<>(0);
    public LiveData<Integer> getStreakCount() { return _streakCount; }


    public ProgressViewModel() {
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    /**
     * Gets the user's profile document from Firestore.
     */
    public void loadUserGoals() {
        if (currentUser == null) return;

        DocumentReference userDocRef = db.collection("users").document(currentUser.getUid());
        userDocRef.get().addOnSuccessListener(document -> {
            if (document.exists()) {
                _userName.setValue(document.getString("firstName"));
                _calorieGoal.setValue(document.getLong("dailyCalorieGoal"));
                _carbGoal.setValue(document.getLong("dailyCarbGoal"));
                _proteinGoal.setValue(document.getLong("dailyProteinGoal"));
                _fatGoal.setValue(document.getLong("dailyFatGoal"));
                Log.d(TAG, "User goals loaded successfully.");
            } else {
                Log.w(TAG, "User document does not exist.");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Error loading user goals", e));
    }

    /**
     * Gets today's daily_log document for the Dashboard.
     */
    public void loadTodaysSummary() {
        if (currentUser == null) return;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todayDateString = LocalDate.now().format(dtf);
        DocumentReference logDocRef = db.collection("users").document(currentUser.getUid())
                .collection("daily_logs").document(todayDateString);

        logDocRef.get().addOnSuccessListener(document -> {
            _todaysLog.setValue(document); // Post the whole document
            Log.d(TAG, "Today's log loaded. Exists: " + document.exists());
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading today's log", e);
            _todaysLog.setValue(null); // Post null on failure
        });
    }


    /**
     * Call this to save all goals at once.
     */
    public void saveGoals(int calories, int carbs, int protein, int fat) {
        if (currentUser == null) {
            Log.e(TAG, "Cannot save goals, user is null.");
            return;
        }

        DocumentReference userDocRef = db.collection("users").document(currentUser.getUid());

        Map<String, Object> goals = new HashMap<>();
        goals.put("dailyCalorieGoal", calories);
        goals.put("dailyCarbGoal", carbs);
        goals.put("dailyProteinGoal", protein);
        goals.put("dailyFatGoal", fat);

        userDocRef.update(goals)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Goals successfully saved to Firestore!");
                    // Now, reload the goals to update all observers
                    loadUserGoals();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error saving goals", e));
    }

    /**
     * Loads the last 7 days of data AND calculates the new conditional streak.
     */
    public void loadChartData() {
        if (currentUser == null) {
            Log.w(TAG, "Cannot load chart data, user is null.");
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.e(TAG, "Chart loading requires API 26+");
            return;
        }

        // --- 1. Get the calorie goal first ---
        Long calorieGoal = _calorieGoal.getValue();
        if (calorieGoal == null || calorieGoal == 0) {
            // Goal isn't loaded or set, so streak must be 0.
            Log.w(TAG, "Calorie goal not set. Loading charts, streak will be 0.");
            loadChartDataOnly(); // Call a helper to just load charts
            _streakCount.setValue(0);
            return;
        }

        final long goal = calorieGoal;
        final long minCalories = goal - 100;
        final long maxCalories = goal + 100;
        Log.d(TAG, "Calculating streak with goal range: " + minCalories + " - " + maxCalories);

        DocumentReference userDocRef = db.collection("users").document(currentUser.getUid());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter labelDtf = DateTimeFormatter.ofPattern("E");

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        String[] labels = new String[7];

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateString = date.format(dtf);
            String dayLabel = date.format(labelDtf);
            int chartIndex = 6 - i;
            labels[chartIndex] = dayLabel;
            DocumentReference dayLogRef = userDocRef.collection("daily_logs").document(dateString);
            tasks.add(dayLogRef.get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            Log.d(TAG, "Successfully fetched " + results.size() + " daily logs for charts.");

            ArrayList<Entry> calorieEntries = new ArrayList<>();
            ArrayList<Entry> carbEntries = new ArrayList<>();
            ArrayList<Entry> proteinEntries = new ArrayList<>();
            ArrayList<Entry> fatEntries = new ArrayList<>();

            // --- 2. New Streak Calculation Logic ---
            int currentStreak = 0;
            // Loop backwards from today (index 6) to 7 days ago (index 0)
            for (int i = results.size() - 1; i >= 0; i--) {
                DocumentSnapshot document = (DocumentSnapshot) results.get(i);

                if (document.exists() && document.contains("totalCalories")) {
                    long caloriesEaten = document.getLong("totalCalories");

                    // Check if calories are in the goal range
                    if (caloriesEaten >= minCalories && caloriesEaten <= maxCalories) {
                        // Success! Day counts.
                        currentStreak++;
                    } else {
                        // Log exists but is outside the range. Streak is broken.
                        break;
                    }
                } else {
                    // No log exists for this day. Streak is broken.
                    break;
                }
            }
            _streakCount.setValue(currentStreak);
            Log.d(TAG, "Streak calculated: " + currentStreak);
            // --- End of New Streak Logic ---

            // (Chart data processing is unchanged)
            for (int i = 0; i < results.size(); i++) {
                DocumentSnapshot document = (DocumentSnapshot) results.get(i);
                float xValue = (float) i;
                if (document.exists()) {
                    calorieEntries.add(new Entry(xValue, document.contains("totalCalories") ? document.getLong("totalCalories").floatValue() : 0f));
                    carbEntries.add(new Entry(xValue, document.contains("totalCarbs") ? document.getLong("totalCarbs").floatValue() : 0f));
                    proteinEntries.add(new Entry(xValue, document.contains("totalProtein") ? document.getLong("totalProtein").floatValue() : 0f));
                    fatEntries.add(new Entry(xValue, document.contains("totalFat") ? document.getLong("totalFat").floatValue() : 0f));
                } else {
                    calorieEntries.add(new Entry(xValue, 0f));
                    carbEntries.add(new Entry(xValue, 0f));
                    proteinEntries.add(new Entry(xValue, 0f));
                    fatEntries.add(new Entry(xValue, 0f));
                }
            }

            _chartLabels.setValue(labels);
            _calorieHistory.setValue(calorieEntries);
            _carbHistory.setValue(carbEntries);
            _proteinHistory.setValue(proteinEntries);
            _fatHistory.setValue(fatEntries);

        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching chart data batch", e));
    }

    /**
     * Helper method to load only chart data if streak can't be calculated.
     */
    private void loadChartDataOnly() {
        if (currentUser == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        DocumentReference userDocRef = db.collection("users").document(currentUser.getUid());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter labelDtf = DateTimeFormatter.ofPattern("E");
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        String[] labels = new String[7];

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateString = date.format(dtf);
            String dayLabel = date.format(labelDtf);
            int chartIndex = 6 - i;
            labels[chartIndex] = dayLabel;
            DocumentReference dayLogRef = userDocRef.collection("daily_logs").document(dateString);
            tasks.add(dayLogRef.get());
        }

        Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
            ArrayList<Entry> calorieEntries = new ArrayList<>();
            ArrayList<Entry> carbEntries = new ArrayList<>();
            ArrayList<Entry> proteinEntries = new ArrayList<>();
            ArrayList<Entry> fatEntries = new ArrayList<>();

            for (int i = 0; i < results.size(); i++) {
                DocumentSnapshot document = (DocumentSnapshot) results.get(i);
                float xValue = (float) i;
                if (document.exists()) {
                    calorieEntries.add(new Entry(xValue, document.contains("totalCalories") ? document.getLong("totalCalories").floatValue() : 0f));
                    carbEntries.add(new Entry(xValue, document.contains("totalCarbs") ? document.getLong("totalCarbs").floatValue() : 0f));
                    proteinEntries.add(new Entry(xValue, document.contains("totalProtein") ? document.getLong("totalProtein").floatValue() : 0f));
                    fatEntries.add(new Entry(xValue, document.contains("totalFat") ? document.getLong("totalFat").floatValue() : 0f));
                } else {
                    calorieEntries.add(new Entry(xValue, 0f));
                    carbEntries.add(new Entry(xValue, 0f));
                    proteinEntries.add(new Entry(xValue, 0f));
                    fatEntries.add(new Entry(xValue, 0f));
                }
            }
            _chartLabels.setValue(labels);
            _calorieHistory.setValue(calorieEntries);
            _carbHistory.setValue(carbEntries);
            _proteinHistory.setValue(proteinEntries);
            _fatHistory.setValue(fatEntries);
        }).addOnFailureListener(e -> Log.e(TAG, "Error fetching chart data batch", e));
    }
}

//Used Gemini AI for Genarations and Error Handlings
