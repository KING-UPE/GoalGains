package com.example.myapplication;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast; // <-- *** ADD TOAST IMPORT ***

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;

// *** ADD FIREBASE AUTH IMPORT ***
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class

ProgressFragment extends Fragment {

    private ProgressViewModel progressViewModel;
    private NavController navController;

    // *** ADD FIREBASE AUTH VARIABLE ***
    private FirebaseAuth mAuth;

    // Views for "No Goal" state
    private LinearLayout layoutNoGoal;
    private Button btnSetAutomaticGoal;
    private Button btnSetManualGoal;

    // Views for "Goal Set" state
    private LinearLayout layoutGoalSet;
    private Button btnEditGoal;
    private TextView tvGoalCalories;

    // Goal Macro Views
    private View goalCarbsView;
    private View goalProteinView;
    private View goalFatView;

    // Chart Views
    private LineChart chartCalories;
    private LineChart chartCarbs;
    private LineChart chartProtein;
    private LineChart chartFat;

    // Chart labels (e.g., "Mon", "Tue")
    private String[] chartXAxisLabels = {"", "", "", "", "", "", ""};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        progressViewModel = new ViewModelProvider(requireActivity()).get(ProgressViewModel.class);

        // *** INITIALIZE FIREBASE AUTH ***
        mAuth = FirebaseAuth.getInstance();

        findViews(view);
        setupGoalObservers();
        setupChartObservers();

        progressViewModel.loadUserGoals();
        progressViewModel.loadChartData();

        // *** MODIFIED CLICK LISTENER ***
        btnSetAutomaticGoal.setOnClickListener(v -> {
            // Check for user *before* navigating
            if (mAuth.getCurrentUser() != null) {
                navController.navigate(R.id.action_navigation_progress_to_goalSetupAutoFragment);
            } else {
                showLoginPrompt();
            }
        });

        // *** MODIFIED CLICK LISTENER ***
        btnSetManualGoal.setOnClickListener(v -> {
            // Check for user *before* navigating
            if (mAuth.getCurrentUser() != null) {
                navController.navigate(R.id.action_navigation_progress_to_goalSetupManualFragment);
            } else {
                showLoginPrompt();
            }
        });

        // (This click listener is inside showEditModeDialog())
        btnEditGoal.setOnClickListener(v -> {
            showEditModeDialog();
        });
    }

    // *** NEW HELPER METHOD ***
    private void showLoginPrompt() {
        Toast.makeText(getContext(), "Please log in to set a goal", Toast.LENGTH_SHORT).show();
        // Use the global action to navigate to the auth choice screen
        navController.navigate(R.id.action_global_authChoiceFragment);
    }

    private void findViews(View view) {
        // State 1: No Goal
        layoutNoGoal = view.findViewById(R.id.layout_no_goal);
        btnSetAutomaticGoal = view.findViewById(R.id.btnSetAutomaticGoal);
        btnSetManualGoal = view.findViewById(R.id.btnSetManualGoal);

        // State 2: Goal Set
        layoutGoalSet = view.findViewById(R.id.layout_goal_set);
        btnEditGoal = view.findViewById(R.id.btnEditGoal);
        tvGoalCalories = view.findViewById(R.id.tvGoalCalories);

        // Macro goal displays
        goalCarbsView = view.findViewById(R.id.goalCarbs);
        goalProteinView = view.findViewById(R.id.goalProtein);
        goalFatView = view.findViewById(R.id.goalFat);

        // Charts
        View chartsLayout = view.findViewById(R.id.charts_layout);
        chartCalories = chartsLayout.findViewById(R.id.chartCalories);
        chartCarbs = chartsLayout.findViewById(R.id.chartCarbs);
        chartProtein = chartsLayout.findViewById(R.id.chartProtein);
        chartFat = chartsLayout.findViewById(R.id.chartFat);
    }

    private void setupGoalObservers() {
        progressViewModel.getCalorieGoal().observe(getViewLifecycleOwner(), calorieGoal -> {
            if (calorieGoal == null || calorieGoal == 0) {
                layoutNoGoal.setVisibility(View.VISIBLE);
                layoutGoalSet.setVisibility(View.GONE);
            } else {
                layoutNoGoal.setVisibility(View.GONE);
                layoutGoalSet.setVisibility(View.VISIBLE);
                tvGoalCalories.setText(String.format(Locale.US, "%,d kcal", calorieGoal));
            }
        });

        // *** THESE LINES ARE FIXED ***
        // Pass your R.drawable circle files
        progressViewModel.getCarbGoal().observe(getViewLifecycleOwner(), carbs ->
                updateMacroView(goalCarbsView, "Carbs", carbs, R.drawable.color_ball_carbs));

        progressViewModel.getProteinGoal().observe(getViewLifecycleOwner(), protein ->
                updateMacroView(goalProteinView, "Protein", protein, R.drawable.color_ball_protein));

        progressViewModel.getFatGoal().observe(getViewLifecycleOwner(), fat ->
                updateMacroView(goalFatView, "Fat", fat, R.drawable.color_ball_fat));
    }

    private void setupChartObservers() {
        progressViewModel.getChartLabels().observe(getViewLifecycleOwner(), labels -> {
            if (labels != null) {
                this.chartXAxisLabels = labels;
                setupAllCharts();
            }
        });

        progressViewModel.getCalorieHistory().observe(getViewLifecycleOwner(), entries ->
                setupLineChart(chartCalories, entries, "Calories", R.color.primary_brand,
                        progressViewModel.getCalorieGoal().getValue()));

        progressViewModel.getCarbHistory().observe(getViewLifecycleOwner(), entries ->
                setupLineChart(chartCarbs, entries, "Carbs", R.color.macro_carbs,
                        progressViewModel.getCarbGoal().getValue()));

        progressViewModel.getProteinHistory().observe(getViewLifecycleOwner(), entries ->
                setupLineChart(chartProtein, entries, "Protein", R.color.macro_protein,
                        progressViewModel.getProteinGoal().getValue()));

        // *** THIS IS THE COMPLETED SECTION THAT WAS CUT OFF ***
        progressViewModel.getFatHistory().observe(getViewLifecycleOwner(), entries ->
                setupLineChart(chartFat, entries, "Fat", R.color.macro_fat,
                        progressViewModel.getFatGoal().getValue()));
    }


    private void updateMacroView(View macroView, String label, Long value, int drawableResId) {
        if (value == null || getContext() == null) return;
        TextView tvLabel = macroView.findViewById(R.id.tvMacroLabel);
        TextView tvValue = macroView.findViewById(R.id.tvMacroValue);
        View macroColorDot = macroView.findViewById(R.id.macroColorDot);

        tvLabel.setText(label);
        tvValue.setText(String.format(Locale.US, "%dg", value));

        // *** THIS LINE IS FIXED ***
        // This now sets the background to your specific circle drawable file
        macroColorDot.setBackgroundResource(drawableResId);
    }

    private void setupAllCharts() {
        setupLineChart(chartCalories, progressViewModel.getCalorieHistory().getValue(), "Calories", R.color.primary_brand, progressViewModel.getCalorieGoal().getValue());
        setupLineChart(chartCarbs, progressViewModel.getCarbHistory().getValue(), "Carbs", R.color.macro_carbs, progressViewModel.getCarbGoal().getValue());
        setupLineChart(chartProtein, progressViewModel.getProteinHistory().getValue(), "Protein", R.color.macro_protein, progressViewModel.getProteinGoal().getValue());
        setupLineChart(chartFat, progressViewModel.getFatHistory().getValue(), "Fat", R.color.macro_fat, progressViewModel.getFatGoal().getValue());
    }

    private void setupLineChart(LineChart chart, List<Entry> entries, String label, int colorResId, Long goalValue) {
        if (entries == null || entries.isEmpty() || getContext() == null) {
            chart.clear();
            chart.invalidate();
            return;
        }

        float goal = (goalValue != null) ? goalValue.floatValue() : 0f;
        int chartColor = getContext().getColor(colorResId);

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(chartColor);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleColor(chartColor);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < chartXAxisLabels.length) {
                    return chartXAxisLabels[index];
                }
                return "";
            }
        });

        YAxis yAxisLeft = chart.getAxisLeft();
        yAxisLeft.setTextColor(Color.GRAY);
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setGridColor(Color.parseColor("#33FFFFFF"));
        yAxisLeft.setAxisLineColor(Color.TRANSPARENT);
        yAxisLeft.setAxisMinimum(0f);

        chart.getAxisRight().setEnabled(false);

        yAxisLeft.removeAllLimitLines();
        if (goal > 0) {
            LimitLine goalLine = new LimitLine(goal, "Goal");
            goalLine.setLineWidth(2f);
            goalLine.setLineColor(Color.GREEN);
            goalLine.enableDashedLine(10f, 10f, 0f);
            goalLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
            goalLine.setTextColor(Color.GREEN);
            goalLine.setTextSize(10f);
            yAxisLeft.addLimitLine(goalLine);
        }

        chart.invalidate();
    }


    /**
     * This is the method for the beautiful custom dialog
     */
    private void showEditModeDialog() {
        if (getContext() == null) return;

        // 1. Inflate the custom layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_goal, null);

        // 2. Find the buttons in the custom layout
        MaterialButton btnAuto = dialogView.findViewById(R.id.btnDialogAutomatic);
        MaterialButton btnManual = dialogView.findViewById(R.id.btnDialogManual);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnDialogCancel);

        // 3. Create the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        // 4. Set the background to transparent for the rounded corners to show
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // 5. Set click listeners
        btnAuto.setOnClickListener(v -> {
            // *** ADDED AUTH CHECK ***
            if (mAuth.getCurrentUser() != null) {
                navController.navigate(R.id.action_navigation_progress_to_goalSetupAutoFragment);
            } else {
                showLoginPrompt();
            }
            dialog.dismiss();
        });

        btnManual.setOnClickListener(v -> {
            // *** ADDED AUTH CHECK ***
            if (mAuth.getCurrentUser() != null) {
                navController.navigate(R.id.action_navigation_progress_to_goalSetupManualFragment);
            } else {
                showLoginPrompt();
            }
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
        });

        // 6. Show the dialog
        dialog.show();
    }
}

//Used Gemini AI for Genarations and Error Handlings
