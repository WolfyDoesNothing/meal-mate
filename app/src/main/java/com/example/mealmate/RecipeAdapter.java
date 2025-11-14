package com.example.mealmate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
        void onTrackToggled(Recipe recipe, boolean isChecked);
    }

    private List<Recipe> recipeList;
    private OnRecipeClickListener listener;

    public RecipeAdapter(Context context, List<Recipe> recipeList, OnRecipeClickListener listener) {
        this.recipeList = recipeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recipe_item, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);
        holder.textRecipeName.setText(recipe.getName());
        holder.checkTrack.setChecked(recipe.isTracked());

        //star icon when favorited
        holder.imageFavorite.setVisibility(recipe.isFavorited() ? View.VISIBLE : View.GONE);

        holder.checkTrack.setOnCheckedChangeListener((buttonView, isChecked) -> {
            recipe.setTracked(isChecked);
            listener.onTrackToggled(recipe, isChecked);
        });

        holder.itemView.setOnClickListener(v -> listener.onRecipeClick(recipe));
    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    public void removeItem(int position) {
        recipeList.remove(position);
        notifyItemRemoved(position);
    }

    public Recipe getItem(int position) {
        return recipeList.get(position);
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView textRecipeName;
        CheckBox checkTrack;
        ImageView imageFavorite;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            textRecipeName = itemView.findViewById(R.id.textRecipeName);
            checkTrack = itemView.findViewById(R.id.checkTrack);
            imageFavorite = itemView.findViewById(R.id.imageFavorite);
        }
    }
}
