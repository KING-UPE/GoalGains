package com.example.myapplication;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.time.LocalDate;

/**
 * An Activity-scoped ViewModel to communicate the selected date/meal
 * from MealsFragment to MainActivity.
 * MainActivity reads this ViewModel when the FAB is clicked
 * to know which date/meal to pass to the AddMealFragment.
 */
public class MealsViewModel extends ViewModel {

    // Holds the date selected in the MealsFragment calendar
    private final MutableLiveData<LocalDate> _selectedDate = new MutableLiveData<>(LocalDate.now());
    public final LiveData<LocalDate> selectedDate = _selectedDate;

    // Holds the meal type selected in the MealsFragment tabs
    private final MutableLiveData<String> _selectedMealType = new MutableLiveData<>("Breakfast");
    public final LiveData<String> selectedMealType = _selectedMealType;

    /**
     * Called by MealsFragment when the user selects a new date.
     */
    public void setSelectedDate(LocalDate date) {
        _selectedDate.setValue(date);
    }

    /**
     * Called by MealsFragment when the user selects a new tab.
     */
    public void setSelectedMealType(String mealType) {
        _selectedMealType.setValue(mealType);
    }
}

//Used Gemini AI for Genarations and Error Handlings
