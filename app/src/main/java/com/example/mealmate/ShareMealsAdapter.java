package com.example.mealmate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShareMealsAdapter extends RecyclerView.Adapter<ShareMealsAdapter.ShareMealViewHolder> {

    private final List<Recipe> meals;
    private final Set<Recipe> selectedMeals = new HashSet<>();

    public ShareMealsAdapter(List<Recipe> meals) {
        this.meals = meals;
    }

    @NonNull
    @Override
    public ShareMealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_share_meal, parent, false);
        return new ShareMealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShareMealViewHolder holder, int position) {
        Recipe recipe = meals.get(position);
        holder.checkBox.setText(recipe.getName());
        holder.checkBox.setChecked(selectedMeals.contains(recipe));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedMeals.add(recipe);
            } else {
                selectedMeals.remove(recipe);
            }
        });
    }

    @Override
    public int getItemCount() {
        return meals.size();
    }

    public List<Recipe> getSelectedMeals() {
        return List.copyOf(selectedMeals);
    }

    static class ShareMealViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;

        public ShareMealViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkboxMeal);
        }
    }
}
