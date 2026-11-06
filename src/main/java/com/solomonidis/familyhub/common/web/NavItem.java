package com.solomonidis.familyhub.common.web;

import java.util.List;

/**
 * One section of the site. {@code enabled=false} renders the link as "coming soon";
 * flip it when the feature's milestone lands. {@code bottomBar} items appear in the phone tab bar.
 */
public record NavItem(String key, String label, String icon, String path, boolean enabled, boolean bottomBar) {

    public static final List<NavItem> ALL = List.of(
        new NavItem("home", "Home", "house", "/", true, true),
        new NavItem("chores", "Chores", "check2-square", "/chores", false, true),
        new NavItem("groceries", "Groceries", "cart3", "/groceries", false, true),
        new NavItem("meal-plan", "Meals", "egg-fried", "/meal-plan", false, true),
        new NavItem("recipes", "Recipes", "book", "/recipes", false, false),
        new NavItem("calendar", "Calendar", "calendar3", "/calendar", false, true),
        new NavItem("goals", "Goals", "bullseye", "/goals", false, true),
        new NavItem("members", "Members", "people", "/members", true, false));
}
