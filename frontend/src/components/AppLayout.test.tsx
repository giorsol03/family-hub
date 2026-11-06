import { screen, within } from '@testing-library/react'
import { Route, Routes } from 'react-router'
import { describe, expect, it } from 'vitest'
import { renderWithProviders } from '../test/render'
import { AppLayout } from './AppLayout'
import { NAV_ITEMS, isActive } from './navItems'

function renderLayout(route: string) {
  return renderWithProviders(
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="*" element={<p>page content</p>} />
      </Route>
    </Routes>,
    { route },
  )
}

describe('AppLayout', () => {
  it('renders the page inside the shell with the current member', () => {
    renderLayout('/')

    expect(screen.getByText('page content')).toBeInTheDocument()
    expect(screen.getByLabelText('Account menu')).toHaveTextContent('Admin')
  })

  it('links enabled sections and disables the ones not built yet', () => {
    renderLayout('/')
    const sidebar = screen.getByLabelText('Sections')

    expect(within(sidebar).getByRole('link', { name: /Members/ })).toHaveAttribute('href', '/members')
    expect(within(sidebar).getByRole('link', { name: /Groceries/ })).toHaveAttribute('href', '/groceries')
    expect(within(sidebar).queryByRole('link', { name: /Goals/ })).not.toBeInTheDocument()
    expect(within(sidebar).getAllByText('Soon').length).toBe(NAV_ITEMS.filter((i) => !i.enabled).length)
  })
})

describe('isActive', () => {
  const home = NAV_ITEMS.find((i) => i.key === 'home')!
  const members = NAV_ITEMS.find((i) => i.key === 'members')!

  it('matches home only on the root path', () => {
    expect(isActive(home, '/')).toBe(true)
    expect(isActive(home, '/members')).toBe(false)
  })

  it('matches a section and its sub-pages but not look-alike paths', () => {
    expect(isActive(members, '/members')).toBe(true)
    expect(isActive(members, '/members/3')).toBe(true)
    expect(isActive(members, '/membership')).toBe(false)
  })
})
