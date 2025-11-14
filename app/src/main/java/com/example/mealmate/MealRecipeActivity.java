package com.example.mealmate;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MealRecipeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private FloatingActionButton fabAddRecipe;

    private List<Recipe> recipeList = new ArrayList<>();
    private RecipeAdapter adapter;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_recipe);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Toolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, topAppBar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        navigationView.setNavigationItemSelectedListener(this::handleNavigation);

        fabAddRecipe = findViewById(R.id.fabAddRecipe);
        fabAddRecipe.setOnClickListener(v -> startActivity(new Intent(this, AddRecipeActivity.class)));

        recyclerView = findViewById(R.id.recyclerViewRecipes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RecipeAdapter(this, recipeList, new RecipeAdapter.OnRecipeClickListener() {
            @Override
            public void onRecipeClick(Recipe recipe) {
                Intent intent = new Intent(MealRecipeActivity.this, RecipeDetailActivity.class);
                intent.putExtra("id", recipe.getId());
                intent.putExtra("name", recipe.getName());
                intent.putExtra("steps", recipe.getSteps());
                intent.putExtra("tracked", recipe.isTracked());
                intent.putExtra("ingredients", new ArrayList<>(recipe.getIngredients()));
                startActivity(intent);
            }

            @Override
            public void onTrackToggled(Recipe recipe, boolean isChecked) {
                recipe.setTracked(isChecked);
                updateTrackingInFirestore(recipe);
            }
        });
        recyclerView.setAdapter(adapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Recipe swipedRecipe = adapter.getItem(position);

                if (direction == ItemTouchHelper.LEFT) {
                    //Delete
                    new AlertDialog.Builder(MealRecipeActivity.this)
                            .setTitle("Delete Recipe")
                            .setMessage("Are you sure you want to delete \"" + swipedRecipe.getName() + "\"?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                deleteRecipeFromFirestore(swipedRecipe);
                                adapter.removeItem(position);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                adapter.notifyItemChanged(position);
                                dialog.dismiss();
                            })
                            .setCancelable(false)
                            .show();
                } else if (direction == ItemTouchHelper.RIGHT) {
                    //Favorite
                    boolean newFavoriteStatus = !swipedRecipe.isFavorited();
                    swipedRecipe.setFavorited(newFavoriteStatus);
                    db.collection("users")
                            .document(userId)
                            .collection("recipes")
                            .document(swipedRecipe.getId())
                            .update("favorited", newFavoriteStatus)
                            .addOnSuccessListener(unused -> {
                                recipeList.sort((r1, r2) -> Boolean.compare(r2.isFavorited(), r1.isFavorited()));
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(MealRecipeActivity.this, "Failed to update favorite", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                Paint paint = new Paint();

                if (dX < 0) {
                    //swipe left to delete
                    paint.setColor(getResources().getColor(android.R.color.holo_red_dark));
                    c.drawRect((float) itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom(), paint);

                    Drawable icon = ContextCompat.getDrawable(MealRecipeActivity.this, R.drawable.ic_delete);
                    if (icon != null) {
                        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + icon.getIntrinsicHeight();
                        int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
                        int iconRight = itemView.getRight() - iconMargin;
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        icon.draw(c);
                    }
                } else if (dX > 0) {
                    //swipe right to favorite
                    paint.setColor(getResources().getColor(android.R.color.holo_orange_dark));
                    c.drawRect(itemView.getLeft(), itemView.getTop(), dX, itemView.getBottom(), paint);

                    Drawable icon = ContextCompat.getDrawable(MealRecipeActivity.this, R.drawable.ic_star);
                    if (icon != null) {
                        int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + icon.getIntrinsicHeight();
                        int iconLeft = itemView.getLeft() + iconMargin;
                        int iconRight = iconLeft + icon.getIntrinsicWidth();
                        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        icon.draw(c);
                    }
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);


        loadRecipesFromFirestore();
    }

    private boolean handleNavigation(@NonNull MenuItem item) {
        drawerLayout.closeDrawers();
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            startActivity(new Intent(this, MainActivity.class));
        } else if (id == R.id.nav_meal_recipes) {
            startActivity(new Intent(this, MealRecipeActivity.class));
        } else if (id == R.id.nav_delegation) {
            startActivity(new Intent(this, ItemDelegationActivity.class));
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        return true;
    }

    private void loadRecipesFromFirestore() {
        db.collection("users").document(userId).collection("recipes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    recipeList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        List<String> ingredients = (List<String>) doc.get("ingredients");
                        String steps = doc.getString("steps");
                        boolean tracked = Boolean.TRUE.equals(doc.getBoolean("tracked"));
                        boolean favorited = Boolean.TRUE.equals(doc.getBoolean("favorited"));

                        Recipe recipe = new Recipe(id, name, ingredients, steps, tracked);
                        recipe.setFavorited(favorited);
                        recipeList.add(recipe);
                    }

                    Collections.sort(recipeList, (r1, r2) -> Boolean.compare(r2.isFavorited(), r1.isFavorited()));

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading recipes: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateTrackingInFirestore(Recipe recipe) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("tracked", recipe.isTracked());

        if (recipe.isTracked()) {
            updates.put("purchasedMap", new HashMap<String, Boolean>());
        }

        db.collection("users").document(userId).collection("recipes").document(recipe.getId()).update(updates);
    }

    private void updateFavoriteInFirestore(Recipe recipe) {
        db.collection("users")
                .document(userId)
                .collection("recipes")
                .document(recipe.getId())
                .update("favorite", recipe.isFavorited());
    }


    private void deleteRecipeFromFirestore(Recipe recipe) {
        db.collection("users").document(userId).collection("recipes").document(recipe.getId()).delete();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecipesFromFirestore();
    }
}
