import { Route, Routes } from 'react-router'
import { AppLayout } from './components/AppLayout'
import { NotFoundPage } from './components/NotFoundPage'
import { LoginPage } from './features/auth/LoginPage'
import { RequireAuth } from './features/auth/RequireAuth'
import { HomePage } from './features/home/HomePage'
import { MemberFormPage } from './features/members/MemberFormPage'
import { MembersPage } from './features/members/MembersPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route index element={<HomePage />} />
          <Route path="members" element={<MembersPage />} />
          <Route path="members/new" element={<MemberFormPage />} />
          <Route path="members/:id" element={<MemberFormPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  )
}
