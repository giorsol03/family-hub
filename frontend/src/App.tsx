import { Route, Routes } from 'react-router'
import { AppLayout } from './components/AppLayout'
import { NotFoundPage } from './components/NotFoundPage'
import { LoginPage } from './features/auth/LoginPage'
import { CalendarPage } from './features/calendar/CalendarPage'
import { ChoresPage } from './features/chores/ChoresPage'
import { ChoreTemplatesPage } from './features/chores/ChoreTemplatesPage'
import { RequireAuth } from './features/auth/RequireAuth'
import { GroceryDefaultsPage } from './features/groceries/GroceryDefaultsPage'
import { GroceryListPage } from './features/groceries/GroceryListPage'
import { HomePage } from './features/home/HomePage'
import { MemberFormPage } from './features/members/MemberFormPage'
import { MealPlanPage } from './features/mealplan/MealPlanPage'
import { MembersPage } from './features/members/MembersPage'
import { RecipeDetailPage } from './features/recipes/RecipeDetailPage'
import { RecipeFormPage } from './features/recipes/RecipeFormPage'
import { RecipesPage } from './features/recipes/RecipesPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route index element={<HomePage />} />
          <Route path="chores" element={<ChoresPage />} />
          <Route path="chores/templates" element={<ChoreTemplatesPage />} />
          <Route path="groceries" element={<GroceryListPage />} />
          <Route path="groceries/defaults" element={<GroceryDefaultsPage />} />
          <Route path="meal-plan" element={<MealPlanPage />} />
          <Route path="calendar" element={<CalendarPage />} />
          <Route path="recipes" element={<RecipesPage />} />
          <Route path="recipes/new" element={<RecipeFormPage />} />
          <Route path="recipes/:id" element={<RecipeDetailPage />} />
          <Route path="recipes/:id/edit" element={<RecipeFormPage />} />
          <Route path="members" element={<MembersPage />} />
          <Route path="members/new" element={<MemberFormPage />} />
          <Route path="members/:id" element={<MemberFormPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
