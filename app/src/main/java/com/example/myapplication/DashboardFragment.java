package com.example.myapplication;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {

    // --- EXISTING VIEWS ---
    private CircularProgressIndicator progressRing;
    private TextView tvProgressPercentage, tvCaloriesLeftValue, tvProgressDate;
    private LinearProgressIndicator progressCarbs, progressProtein, progressFat;
    private TextView tvCarbsValue, tvProteinValue, tvFatValue;
    private TextView tvUserName;
    private MaterialCardView streakCard;
    private TextView tvStreakTitle;
    private TextView tvStreakSubtitle;

    // --- NEW VIEWS FOR APIs ---
    private TextView tvQuoteText, tvQuoteAuthor;
    private Button btnRandomRecipe;

    // --- LOGIC VARIABLES ---
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private NavController navController;
    private ProgressViewModel progressViewModel;

    // --- THREADING FOR APIs ---
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        navController = Navigation.findNavController(view);
        progressViewModel = new ViewModelProvider(requireActivity()).get(ProgressViewModel.class);

        findViews(view);
        setupClickListeners(view);

        // --- 1. CALL API 1 (ZEN QUOTES) ---
        fetchDailyQuote();

        // --- 2. SETUP API 2 LISTENER (RECIPE) ---
        btnRandomRecipe.setOnClickListener(v -> fetchRandomRecipe());

        // --- 3. LOAD EXISTING FIREBASE DATA ---
        if (currentUser != null) {
            setupObservers();
            progressViewModel.loadUserGoals();
            progressViewModel.loadTodaysSummary();
        } else {
            tvUserName.setText("Hello, Guest");
            streakCard.setVisibility(View.GONE);
            startAnimations(0, 0, 0, "Today", 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    private void findViews(View view) {
        // Old Views
        tvUserName = view.findViewById(R.id.tvUserName);
        progressRing = view.findViewById(R.id.progressRing);
        tvProgressPercentage = view.findViewById(R.id.tvProgressPercentage);
        tvCaloriesLeftValue = view.findViewById(R.id.tvCaloriesLeftValue);
        tvProgressDate = view.findViewById(R.id.tvProgressDate);
        progressCarbs = view.findViewById(R.id.progressCarbs);
        progressProtein = view.findViewById(R.id.progressProtein);
        progressFat = view.findViewById(R.id.progressFat);
        tvCarbsValue = view.findViewById(R.id.tvCarbsValue);
        tvProteinValue = view.findViewById(R.id.tvProteinValue);
        tvFatValue = view.findViewById(R.id.tvFatValue);
        streakCard = view.findViewById(R.id.streakCard);
        tvStreakTitle = view.findViewById(R.id.tvStreakTitle);
        tvStreakSubtitle = view.findViewById(R.id.tvStreakSubtitle);

        // New Views for APIs
        tvQuoteText = view.findViewById(R.id.tvQuoteText);
        tvQuoteAuthor = view.findViewById(R.id.tvQuoteAuthor);
        btnRandomRecipe = view.findViewById(R.id.btnRandomRecipe);
    }

    private void setupClickListeners(View view) {
        MaterialCardView searchBar = view.findViewById(R.id.searchBar);
        searchBar.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("focus_search", true);
            navController.navigate(R.id.action_dashboard_to_foods, args);
        });
        ImageView ivProfile = view.findViewById(R.id.ivProfile);
        ivProfile.setOnClickListener(v -> {
            if (currentUser != null) {
                navController.navigate(R.id.action_dashboardFragment_to_profileFragment);
            } else {
                navController.navigate(R.id.action_dashboardFragment_to_authChoiceFragment);
            }
        });
    }

    // ==================================================
    // API 1: ZENQUOTES IMPLEMENTATION
    // ==================================================
    private void fetchDailyQuote() {
        executor.execute(() -> {
            try {
                URL url = new URL("https://zenquotes.io/api/today");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(result.toString());
                if (jsonArray.length() > 0) {
                    JSONObject firstQuote = jsonArray.getJSONObject(0);
                    String q = firstQuote.getString("q");
                    String a = firstQuote.getString("a");

                    handler.post(() -> {
                        if (tvQuoteText != null) {
                            tvQuoteText.setText("\"" + q + "\"");
                            tvQuoteAuthor.setText("- " + a);
                        }
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    if (tvQuoteText != null) tvQuoteText.setText("Believe in yourself! (Offline Mode)");
                });
            }
        });
    }

    // ==================================================
    // API 2: THEMEALDB IMPLEMENTATION
    // ==================================================
    private void fetchRandomRecipe() {
        Toast.makeText(getContext(), "Finding a recipe...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                URL url = new URL("https://www.themealdb.com/api/json/v1/1/random.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                JSONObject response = new JSONObject(result.toString());
                JSONArray meals = response.getJSONArray("meals");
                if (meals.length() > 0) {
                    JSONObject meal = meals.getJSONObject(0);
                    String name = meal.getString("strMeal");
                    String category = meal.getString("strCategory");
                    String thumb = meal.getString("strMealThumb");

                    handler.post(() -> showRecipeDialog(name, category, thumb));
                }

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(getContext(), "Error fetching recipe.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showRecipeDialog(String name, String category, String imageUrl) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_recipe_api, null);

        TextView tvName = dialogView.findViewById(R.id.tvApiRecipeName);
        TextView tvCat = dialogView.findViewById(R.id.tvApiRecipeCategory);
        ImageView ivImg = dialogView.findViewById(R.id.ivApiRecipeImage);
        Button btnClose = dialogView.findViewById(R.id.btnApiClose);

        tvName.setText(name);
        tvCat.setText("Category: " + category);
        Glide.with(this).load(imageUrl).into(ivImg);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ==================================================
    // EXISTING DASHBOARD LOGIC (UNCHANGED)
    // ==================================================
    private void setupObservers() {
        progressViewModel.getUserName().observe(getViewLifecycleOwner(), name -> {
            tvUserName.setText("Hello, " + (name != null ? name : "User"));
        });

        progressViewModel.getCalorieGoal().observe(getViewLifecycleOwner(), goal -> {
            progressViewModel.loadChartData();
            refreshDashboardCalculations();
        });

        progressViewModel.getProteinGoal().observe(getViewLifecycleOwner(), goal -> refreshDashboardCalculations());
        progressViewModel.getCarbGoal().observe(getViewLifecycleOwner(), goal -> refreshDashboardCalculations());
        progressViewModel.getFatGoal().observe(getViewLifecycleOwner(), goal -> refreshDashboardCalculations());

        progressViewModel.getTodaysLog().observe(getViewLifecycleOwner(), logDocument -> {
            refreshDashboardCalculations();
        });

        progressViewModel.getStreakCount().observe(getViewLifecycleOwner(), streak -> {
            if (streak == null) return;
            streakCard.setVisibility(View.VISIBLE);
            if (streak == 0) {
                tvStreakTitle.setText("Let's begin a streak!");
                tvStreakSubtitle.setText("Log calories within 100kcal of your goal.");
            } else if (streak == 1) {
                tvStreakTitle.setText("You're on day 1!");
                tvStreakSubtitle.setText("Keep it up to build your streak!");
            } else {
                tvStreakTitle.setText(String.format(Locale.US, "You've hit a %d Day Streak!", streak));
                tvStreakSubtitle.setText("Keep this great momentum going!");
            }
        });
    }

    private void refreshDashboardCalculations() {
        Long calorieGoal = progressViewModel.getCalorieGoal().getValue();
        Long proteinGoal = progressViewModel.getProteinGoal().getValue();
        Long carbGoal = progressViewModel.getCarbGoal().getValue();
        Long fatGoal = progressViewModel.getFatGoal().getValue();
        DocumentSnapshot logDocument = progressViewModel.getTodaysLog().getValue();

        if (calorieGoal == null || proteinGoal == null || carbGoal == null || fatGoal == null) {
            return;
        }

        long calGoal = calorieGoal;
        long proGoal = proteinGoal;
        long carGoal = carbGoal;
        long fatG = fatGoal;

        long caloriesEaten = 0, proteinEaten = 0, carbsEaten = 0, fatEaten = 0;
        if (logDocument != null && logDocument.exists()) {
            if (logDocument.contains("totalCalories")) caloriesEaten = logDocument.getLong("totalCalories");
            if (logDocument.contains("totalProtein")) proteinEaten = logDocument.getLong("totalProtein");
            if (logDocument.contains("totalCarbs")) carbsEaten = logDocument.getLong("totalCarbs");
            if (logDocument.contains("totalFat")) fatEaten = logDocument.getLong("totalFat");
        }

        long caloriesLeft = calGoal - caloriesEaten;
        int ringProgress = (int) (calGoal > 0 ? (caloriesEaten * 100.0) / calGoal : 0);
        int proteinProgress = (int) (proGoal > 0 ? (proteinEaten * 100.0) / proGoal : 0);
        int carbProgress = (int) (carGoal > 0 ? (carbsEaten * 100.0) / carGoal : 0);
        int fatProgress = (int) (fatG > 0 ? (fatEaten * 100.0) / fatG : 0);

        String displayDate = new SimpleDateFormat("dd MMMM yyyy", Locale.US).format(new Date());

        startAnimations(
                ringProgress, ringProgress, (int) caloriesLeft, displayDate,
                carbProgress, (int) carbsEaten, (int) carGoal,
                proteinProgress, (int) proteinEaten, (int) proGoal,
                fatProgress, (int) fatEaten, (int) fatG
        );
    }

    private void startAnimations(int ringProgress, int percentage, int calories, String date,
                                 int carbsProg, int carbsCurrent, int carbsTotal,
                                 int proteinProg, int proteinCurrent, int proteinTotal,
                                 int fatProg, int fatCurrent, int fatTotal) {
        long duration = 1500;
        DecelerateInterpolator interpolator = new DecelerateInterpolator();
        if (progressRing != null) {
            progressRing.setProgressCompat(0, false);
            ValueAnimator ringAnimator = ValueAnimator.ofInt(0, ringProgress);
            ringAnimator.setDuration(duration);
            ringAnimator.setInterpolator(interpolator);
            ringAnimator.addUpdateListener(anim -> {
                int value = (int) anim.getAnimatedValue();
                if (progressRing != null) {
                    progressRing.setProgressCompat(value, false);
                }
                float progressFraction = (ringProgress == 0) ? 0f : (float) value / ringProgress;
                int percentageValue = (int) (progressFraction * percentage);
                if (tvProgressPercentage != null) {
                    tvProgressPercentage.setText(String.format(Locale.US, "%d%%", percentageValue));
                }
            });
            ringAnimator.start();
        }
        if (tvCaloriesLeftValue != null) {
            Long currentGoal = progressViewModel.getCalorieGoal().getValue();
            int startCalories = (currentGoal != null && currentGoal != 0) ? currentGoal.intValue() : 2000;

            ValueAnimator caloriesAnimator = ValueAnimator.ofInt(startCalories, calories);
            caloriesAnimator.setDuration(duration);
            caloriesAnimator.setInterpolator(interpolator);
            caloriesAnimator.addUpdateListener(anim -> {
                int value = (int) anim.getAnimatedValue();
                if (tvCaloriesLeftValue != null) {
                    tvCaloriesLeftValue.setText(String.valueOf(value));
                }
            });
            caloriesAnimator.start();
        }
        if (tvProgressDate != null) {
            tvProgressDate.setText(date);
        }
        if (progressCarbs != null) {
            animateLinearProgress(progressCarbs, carbsProg, duration, interpolator);
        }
        if (progressProtein != null) {
            animateLinearProgress(progressProtein, proteinProg, duration, interpolator);
        }
        if (progressFat != null) {
            animateLinearProgress(progressFat, fatProg, duration, interpolator);
        }
        if (tvCarbsValue != null) {
            animateMacroText(tvCarbsValue, carbsCurrent, carbsTotal, duration, interpolator);
        }
        if (tvProteinValue != null) {
            animateMacroText(tvProteinValue, proteinCurrent, proteinTotal, duration, interpolator);
        }
        if (tvFatValue != null) {
            animateMacroText(tvFatValue, fatCurrent, fatTotal, duration, interpolator);
        }
    }
    private void animateLinearProgress(LinearProgressIndicator progressBar, int targetProgress,
                                       long duration, DecelerateInterpolator interpolator) {
        if (progressBar == null) return;
        progressBar.setProgressCompat(0, false);
        ValueAnimator animator = ValueAnimator.ofInt(0, targetProgress);
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            if (progressBar != null) {
                progressBar.setProgressCompat(value, false);
            }
        });
        animator.start();
    }
    private void animateMacroText(TextView textView, int current, int total,
                                  long duration, DecelerateInterpolator interpolator) {
        if (textView == null) return;
        ValueAnimator animator = ValueAnimator.ofInt(0, current);
        animator.setDuration(duration);
        animator.setInterpolator(interpolator);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            if (textView != null) {
                textView.setText(String.format(Locale.US, "%d / %dg", value, total));
            }
        });
        animator.start();
    }
}

//Used Gemini AI for Genarations and Error Handlings
