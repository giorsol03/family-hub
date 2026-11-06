# Family Hub – Requirements

A private website for our household: chores, groceries, recipes & meal plan, calendar, and daily goals.

## 0. Common

- **Household members** (`Member`): name, color (for UI badges), login (username + password).
- Everyone must log in. All members of the household see the same data.
- All lists support add / edit / delete. Every entity has `createdBy` and `createdAt`.
- **Defaults pattern** (used by Chores and Groceries): a *default* is a reusable template.
  - The app ships with seed defaults.
  - Users can add their own defaults, or deactivate (soft delete) a default that is not relevant at the moment and re-activate it later.
  - A concrete item (a chore to do, an item to buy) can be created from a default in one click, or entered manually.

## 1. Chores

**ChoreTemplate** (default): name, description, default assignee (optional), estimated minutes, recurrence (none / daily / weekly / every N days / monthly), active flag.

Seed defaults: Wash the dishes, Mopping, Vacuuming, Laundry, Take out the rubbish, Clean bathroom, Change bed sheets, Water the plants.

**Chore** (instance): title, description, scheduled date, assignee (Member), status (TODO / DONE / SKIPPED), completedAt, completedBy, optional link to the template it came from.

Behaviour:
- Recurring templates auto-generate the next instance when the previous one is completed.
- Views: Today, This week, Overdue. Filter by assignee.

## 2. Grocery list

**GroceryDefault**: name, category, default quantity, unit, preferred merchant, active flag.

Seed defaults: Milk, Bread, Eggs, Cucumbers, Tomatoes, Flour, Olive oil, Coffee, Toilet paper.

**GroceryItem**: name, category (Dairy, Fruit & Veg, Meat, Bakery, Pantry, Household, Other), quantity (decimal), unit, merchant (e.g. Lidl, Alphamega, Sklavenitis, Metro, Other), notes, bought flag, boughtAt, addedBy, source (MANUAL / DEFAULT / RECIPE).

**Units**: `G`, `KG`, `ML`, `L`, `PCS` (plain count, e.g. 4 cucumbers), `PACK`.

Behaviour:
- Group the list by merchant, then category. Tick to mark as bought; "Clear bought" button.
- **Merging rule**: when an item is added and an unbought item with the same name (case-insensitive) already exists with a compatible unit, increase that item's quantity instead of creating a duplicate.
  - Compatible: G↔KG (1 kg = 1000 g), ML↔L (1 L = 1000 ml); PCS only with PCS; PACK only with PACK.
  - Example: list has 1 kg flour, recipe adds 1 kg flour → 2 kg flour. List has 500 g, recipe adds 1 kg → 1.5 kg.
  - Incompatible units (e.g. 2 PCS vs 500 G) → create a separate line.

## 3. Recipes & weekly meal plan

**Recipe**: title, description, servings, prep time, instructions (text), tags, optional image URL.
**RecipeIngredient**: name, quantity, unit, optional category (used when it becomes a grocery item).

**MealPlanEntry**: date, meal slot (BREAKFAST / LUNCH / DINNER), recipe, planned by.

Behaviour:
- Weekly planner view (Mon–Sun × meal slots); pick a recipe for a slot.
- When a recipe is added to the plan, show a **popup**: "Should we add these ingredients to the grocery list?"
  - Each ingredient has a checkbox (all ticked by default) + "Select all / none".
  - Buttons: Add selected, Skip.
  - Selected ingredients are added to the grocery list using the merging rule above (source = RECIPE).
- Meal plan entries also appear on the Calendar.

## 4. Calendar

**Event**: title, description, start date-time, end date-time (or all-day), location, type (MEAL / EVENT / NIGHT_OUT / OTHER), attendees (many Members), created by.

Behaviour:
- Month and week views; click a day to add an event.
- Meal-plan entries shown automatically (read-only here, edited in the planner).
- Filter by member.

## 5. Daily goals (habit tracker)

**Goal**: name (e.g. Yoga, Eat healthy, Gym, No caffeine), owner (Member who chose it), active flag, start date, optional target (days per week).
**GoalCheck**: goal, date, done.

UI (like a paper habit tracker):
- A grid per month: one row per goal, one column per day (1–31). Click a cell to tick / untick that day.
- Filter by person (dropdown or tabs) so each person's goals aren't mixed up; "Everyone" view groups rows by person.
- Show current streak and % completed this month per goal.
- Add / remove (deactivate) goals.

## 6. Non-functional

- Java 21, Spring Boot 3, Maven, PostgreSQL (Neon), Flyway migrations.
- Server-rendered UI: Thymeleaf + Bootstrap 5 (+ a little htmx/vanilla JS for popups and ticking). Mobile-friendly — it will mostly be used on phones.
- Deployed on Render via Dockerfile; config through env vars.
- Timezone: Europe/Nicosia.
