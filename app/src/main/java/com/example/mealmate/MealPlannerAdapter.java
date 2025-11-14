package com.example.mealmate;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

public class MealPlannerAdapter extends RecyclerView.Adapter<MealPlannerAdapter.MealViewHolder> {

    private final Context context;
    private final List<Recipe> recipeList;
    private final Set<String> expandedIds;

    public MealPlannerAdapter(Context context, List<Recipe> recipeList, Set<String> expandedIds) {
        this.context = context;
        this.recipeList = recipeList;
        this.expandedIds = expandedIds;
    }

    @NonNull
    @Override
    public MealViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.meal_planner_item, parent, false);
        return new MealViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MealViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);
        holder.textMealName.setText(recipe.getName());

        boolean isExpanded = recipe.isExpanded();
        holder.layoutExpandable.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.imageToggle.setImageResource(isExpanded
                ? android.R.drawable.arrow_up_float
                : android.R.drawable.arrow_down_float);

        List<String> ingredientsList = recipe.getIngredients();
        Map<String, Boolean> purchasedMap = recipe.getPurchasedMap();
        if (purchasedMap == null) {
            purchasedMap = new HashMap<>();
            recipe.setPurchasedMap(purchasedMap);
        }

        final Map<String, Boolean> finalPurchasedMap = new HashMap<>(purchasedMap);
        recipe.setPurchasedMap(finalPurchasedMap);

        List<String> purchased = new ArrayList<>();
        List<String> unpurchased = new ArrayList<>();
        for (String ing : ingredientsList) {
            if (Boolean.TRUE.equals(finalPurchasedMap.get(ing))) {
                purchased.add(ing);
            } else {
                unpurchased.add(ing);
            }
        }

        List<String> sortedIngredients = new ArrayList<>();
        sortedIngredients.addAll(unpurchased);
        sortedIngredients.addAll(purchased);

        holder.layoutIngredients.removeAllViews();

        if (recipe.isEditMode()) {
            for (String ing : sortedIngredients) {
                EditText editText = new EditText(context);
                editText.setText(ing);
                editText.setTextSize(18);
                editText.setTextColor(Color.BLACK);
                editText.setInputType(InputType.TYPE_CLASS_TEXT);
                editText.setBackgroundResource(android.R.drawable.editbox_background);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 8);
                editText.setLayoutParams(params);

                holder.layoutIngredients.addView(editText);
            }
        } else {
            for (String ing : sortedIngredients) {
                TextView ingredientView = new TextView(context);
                ingredientView.setText("- " + ing);
                ingredientView.setTextSize(18);
                ingredientView.setPadding(24, 16, 24, 16);
                ingredientView.setBackgroundResource(android.R.drawable.list_selector_background);
                ingredientView.setTextColor(context.getResources().getColor(android.R.color.black));


                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 8);
                ingredientView.setLayoutParams(params);

                if (Boolean.TRUE.equals(finalPurchasedMap.get(ing))) {
                    ingredientView.setPaintFlags(ingredientView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    ingredientView.setAlpha(0.5f);
                }

                ingredientView.setOnClickListener(v -> {
                    boolean isNowPurchased = !Boolean.TRUE.equals(finalPurchasedMap.get(ing));
                    finalPurchasedMap.put(ing, isNowPurchased);
                    recipe.setPurchasedMap(finalPurchasedMap);
                    notifyItemChanged(holder.getAdapterPosition());

                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .collection("recipes")
                            .document(recipe.getId())
                            .update("purchasedMap", finalPurchasedMap);
                });

                holder.layoutIngredients.addView(ingredientView);
            }
        }

        //expand/collapse
        View.OnClickListener toggleListener = v -> {
            recipe.setExpanded(!recipe.isExpanded());
            notifyItemChanged(holder.getAdapterPosition());
        };
        holder.imageToggle.setOnClickListener(toggleListener);
        holder.itemView.setOnClickListener(toggleListener);

        //Edit
        holder.buttonEdit.setText(recipe.isEditMode() ? "Save" : "Edit");
        holder.buttonEdit.setOnClickListener(v -> {
            if (!recipe.isEditMode()) {
                recipe.setEditMode(true);
                notifyItemChanged(holder.getAdapterPosition());
            } else {
                List<String> newIngredients = new ArrayList<>();
                for (int i = 0; i < holder.layoutIngredients.getChildCount(); i++) {
                    View view = holder.layoutIngredients.getChildAt(i);
                    if (view instanceof EditText) {
                        String ing = ((EditText) view).getText().toString().trim();
                        if (!ing.isEmpty()) {
                            newIngredients.add(ing);
                        }
                    }
                }

                recipe.setIngredients(newIngredients);
                finalPurchasedMap.keySet().retainAll(newIngredients);
                recipe.setPurchasedMap(finalPurchasedMap);

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .collection("recipes")
                        .document(recipe.getId())
                        .update("ingredients", newIngredients,
                                "purchasedMap", finalPurchasedMap);

                recipe.setEditMode(false);
                notifyItemChanged(holder.getAdapterPosition());
            }
        });

        //View
        holder.buttonView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RecipeDetailActivity.class);
            intent.putExtra("id", recipe.getId());
            intent.putExtra("name", recipe.getName());
            intent.putExtra("steps", recipe.getSteps());
            intent.putExtra("tracked", recipe.isTracked());
            intent.putExtra("ingredients", new ArrayList<>(recipe.getIngredients()));
            context.startActivity(intent);
        });

        //Complete
        holder.buttonComplete.setOnClickListener(v -> {
            boolean allPurchased = true;
            for (String ing : recipe.getIngredients()) {
                if (!Boolean.TRUE.equals(recipe.getPurchasedMap().get(ing))) {
                    allPurchased = false;
                    break;
                }
            }

            if (allPurchased) {
                new android.app.AlertDialog.Builder(context)
                        .setTitle("🎉 Meal Completed!")
                        .setMessage("You've purchased all ingredients for " + recipe.getName())
                        .setPositiveButton("View Recipe", (dialog, which) -> {
                            recipe.setTracked(false);
                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .collection("recipes")
                                    .document(recipe.getId())
                                    .update("tracked", false);

                            //go to detail page
                            Intent intent = new Intent(context, RecipeDetailActivity.class);
                            intent.putExtra("id", recipe.getId());
                            intent.putExtra("name", recipe.getName());
                            intent.putExtra("steps", recipe.getSteps());
                            intent.putExtra("tracked", false);
                            intent.putExtra("ingredients", new ArrayList<>(recipe.getIngredients()));
                            context.startActivity(intent);
                        })
                        .setNegativeButton("Back to Planner", (dialog, which) -> {
                            recipe.setTracked(false);
                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .collection("recipes")
                                    .document(recipe.getId())
                                    .update("tracked", false);

                            notifyItemChanged(holder.getAdapterPosition());
                        })
                        .setCancelable(true)
                        .show();
            } else {
                Toast.makeText(context, "Please purchase all ingredients first.", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public int getItemCount() {
        return recipeList.size();
    }

    static class MealViewHolder extends RecyclerView.ViewHolder {
        TextView textMealName;
        ImageView imageToggle;
        LinearLayout layoutExpandable, layoutIngredients;
        TextView buttonEdit, buttonView, buttonComplete;

        public MealViewHolder(@NonNull View itemView) {
            super(itemView);
            textMealName = itemView.findViewById(R.id.textMealName);
            imageToggle = itemView.findViewById(R.id.imageToggle);
            layoutExpandable = itemView.findViewById(R.id.layoutExpandable);
            layoutIngredients = itemView.findViewById(R.id.layoutIngredients);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonView = itemView.findViewById(R.id.buttonView);
            buttonComplete = itemView.findViewById(R.id.buttonComplete);
        }
    }
}
