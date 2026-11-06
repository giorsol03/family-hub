import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/client'
import { ADMIN, renderWithProviders } from '../../test/render'
import { MemberForm } from './MemberForm'

describe('MemberForm', () => {
  it('validates required fields before calling the server', async () => {
    const onSubmit = vi.fn()
    renderWithProviders(<MemberForm onSubmit={onSubmit} onCancel={() => {}} />)

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Name is required')).toBeInTheDocument()
    expect(screen.getByText('Password is required')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits trimmed values for a new member', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    renderWithProviders(<MemberForm onSubmit={onSubmit} onCancel={() => {}} />)

    await userEvent.type(screen.getByLabelText(/^Name/), '  Maria ')
    await userEvent.type(screen.getByLabelText(/^Username/), 'maria')
    await userEvent.type(screen.getByLabelText(/^Password/), 'secret-123')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({
      name: 'Maria', username: 'maria', color: '#3b82f6', password: 'secret-123',
    }))
  })

  it('allows a blank password when editing', async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined)
    renderWithProviders(<MemberForm member={ADMIN} onSubmit={onSubmit} onCancel={() => {}} />)

    expect(screen.getByLabelText(/^Name/)).toHaveValue('Admin')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ password: '' })))
  })

  it('shows server field errors on the matching input', async () => {
    const onSubmit = vi.fn().mockRejectedValue(
      new ApiError(409, 'Username is already taken', { username: 'Username is already taken' }),
    )
    renderWithProviders(<MemberForm member={ADMIN} onSubmit={onSubmit} onCancel={() => {}} />)

    await userEvent.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Username is already taken')).toBeInTheDocument()
    expect(screen.getByLabelText(/^Username/)).toHaveAttribute('aria-invalid', 'true')
  })
})
