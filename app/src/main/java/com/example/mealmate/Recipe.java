package com.example.mealmate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Recipe {
    private String id;
    private String name;
    private List<String> ingredients;
    private String steps;
    private boolean tracked;
    private boolean favorited;


    //store purchased state per ingredient to Firebase
    private Map<String, Boolean> purchasedMap = new HashMap<>();

    //expanded/collapsed
    private transient boolean expanded = false;

    //edit mode for Meal Planner
    private transient boolean editMode = false;


    public Recipe() {
        //for Firebase
    }

    public Recipe(String id, String name, List<String> ingredients, String steps, boolean tracked) {
        this.id = id;
        this.name = name;
        this.ingredients = ingredients;
        this.steps = steps;
        this.tracked = tracked;
    }

    //ID
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    //Name
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    //Ingredients
    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    //Steps
    public String getSteps() { return steps; }
    public void setSteps(String steps) { this.steps = steps; }

    //Tracked
    public boolean isTracked() { return tracked; }
    public void setTracked(boolean tracked) { this.tracked = tracked; }

    //Favorite
    public boolean isFavorited() {
        return favorited;
    }

    public void setFavorited(boolean favorite) {
        this.favorited = favorite;
    }


    //Purchased map
    public Map<String, Boolean> getPurchasedMap() { return purchasedMap; }
    public void setPurchasedMap(Map<String, Boolean> purchasedMap) { this.purchasedMap = purchasedMap; }

    //expanded
    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }

    //edit mode
    public boolean isEditMode() { return editMode; }
    public void setEditMode(boolean editMode) { this.editMode = editMode; }

}
