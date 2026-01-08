package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.CalendarDayViewHolder> {

    private List<LocalDate> days;
    private final OnDateClickListener listener;
    private LocalDate selectedDate;

    // Formatter for "Mon", "Tue", etc.
    private final DateTimeFormatter dayNameFormatter = DateTimeFormatter.ofPattern("EEE", Locale.US);

    // Interface for click events
    public interface OnDateClickListener {
        void onDateClick(LocalDate date);
    }

    public CalendarDayAdapter(List<LocalDate> days, LocalDate selectedDate, OnDateClickListener listener) {
        this.days = days;
        this.selectedDate = selectedDate;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarDayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_calendar_day, parent, false);
        return new CalendarDayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarDayViewHolder holder, int position) {
        LocalDate date = days.get(position);
        holder.bind(date, selectedDate, dayNameFormatter, listener);
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    // Helper method to update the list of days
    public void setDays(List<LocalDate> newDays) {
        this.days = newDays;
        notifyDataSetChanged(); // Reload the whole list
    }

    // Helper method to update the selected date
    public void setSelectedDate(LocalDate newSelectedDate) {
        LocalDate oldSelectedDate = this.selectedDate;
        this.selectedDate = newSelectedDate;

        // Find positions to update
        int oldPos = days.indexOf(oldSelectedDate);
        int newPos = days.indexOf(newSelectedDate);

        if (oldPos != -1) {
            notifyItemChanged(oldPos); // Unselect old
        }
        if (newPos != -1) {
            notifyItemChanged(newPos); // Select new
        }
    }


    // ViewHolder class
    static class CalendarDayViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName;
        TextView tvDayNumber;

        public CalendarDayViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tvDayName);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
        }

        public void bind(final LocalDate date, final LocalDate selectedDate,
                         final DateTimeFormatter dayNameFormatter, final OnDateClickListener listener) {

            // Set Day Name (e.g., "Mon")
            tvDayName.setText(date.format(dayNameFormatter));

            // Set Day Number (e.g., "24")
            tvDayNumber.setText(String.valueOf(date.getDayOfMonth()));

            // Set the selected state (this triggers the selectors for background and text color)
            itemView.setSelected(date.equals(selectedDate));

            // Set the click listener
            itemView.setOnClickListener(v -> listener.onDateClick(date));
        }
    }
}
//Used Gemini AI for Genarations and Error Handlings
