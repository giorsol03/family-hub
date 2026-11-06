import { Alert, Autocomplete, Button, Group, NumberInput, Select, Stack, Textarea } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useState, type ReactNode } from 'react'
import { ApiError } from '../../api/client'
import type { GroceryCategory, GroceryDefault, Merchant, QuantityUnit } from '../../api/types'
import { categoryOptions, merchantOptions, unitOptions } from './labels'

/** Shared by the list item form and the favourites (defaults) form. */
export interface GroceryFormValues {
  name: string
  category: GroceryCategory
  quantity: number | string
  unit: QuantityUnit
  merchant: Merchant | null
  notes: string
}

export interface GroceryFormResult {
  name: string
  category: GroceryCategory
  quantity: number
  unit: QuantityUnit
  merchant: Merchant | null
  notes: string | null
}

interface Props {
  initial?: Partial<GroceryFormValues>
  /** Favourites have no notes. */
  withNotes?: boolean
  /** Name suggestions; picking one fills in its category, quantity, unit and shop. */
  suggestions?: GroceryDefault[]
  submitLabel?: string
  onSubmit: (values: GroceryFormResult) => Promise<unknown>
  onCancel: () => void
  /** Extra buttons, e.g. Delete when editing. */
  extraActions?: ReactNode
}

/** Server field names for favourites differ slightly; show their errors on the matching inputs. */
const FIELD_ALIASES: Record<string, string> = { defaultQuantity: 'quantity', preferredMerchant: 'merchant' }

export function GroceryForm({ initial, withNotes = true, suggestions = [], submitLabel = 'Save', onSubmit, onCancel, extraActions }: Props) {
  const [formError, setFormError] = useState<string | null>(null)

  const form = useForm<GroceryFormValues>({
    mode: 'uncontrolled',
    initialValues: {
      name: '',
      category: 'OTHER',
      quantity: 1,
      unit: 'PCS',
      merchant: null,
      notes: '',
      ...initial,
    },
    validate: {
      name: (v) => (!v.trim() ? 'Name is required' : v.length > 100 ? 'Max 100 characters' : null),
      quantity: (v, values) => {
        const n = typeof v === 'number' ? v : Number(v)
        if (v === '' || Number.isNaN(n) || n <= 0) return 'Must be more than 0'
        if ((values.unit === 'PCS' || values.unit === 'PACK') && !Number.isInteger(n)) {
          return 'Use a whole number for pieces and packs'
        }
        return null
      },
      notes: (v) => (v.length > 500 ? 'Max 500 characters' : null),
    },
  })

  function applySuggestion(name: string) {
    const match = suggestions.find((d) => d.name === name)
    if (match) {
      form.setValues({
        name: match.name,
        category: match.category,
        quantity: match.defaultQuantity,
        unit: match.unit,
        merchant: match.preferredMerchant,
      })
    }
  }

  async function handleSubmit(values: GroceryFormValues) {
    setFormError(null)
    try {
      await onSubmit({
        name: values.name.trim(),
        category: values.category,
        quantity: Number(values.quantity),
        unit: values.unit,
        merchant: values.merchant,
        notes: values.notes.trim() || null,
      })
    } catch (e) {
      if (e instanceof ApiError && Object.keys(e.fieldErrors).length > 0) {
        form.setErrors(Object.fromEntries(
          Object.entries(e.fieldErrors).map(([field, message]) => [FIELD_ALIASES[field] ?? field, message]),
        ))
      } else {
        setFormError(e instanceof Error ? e.message : 'Something went wrong')
      }
    }
  }

  const uniqueNames = [...new Set(suggestions.map((d) => d.name))]

  return (
    <form onSubmit={form.onSubmit(handleSubmit)} noValidate>
      <Stack>
        {formError && <Alert color="red" variant="light">{formError}</Alert>}
        <Autocomplete
          label="Item"
          placeholder="e.g. Flour"
          required
          data={uniqueNames}
          onOptionSubmit={applySuggestion}
          data-autofocus
          key={form.key('name')}
          {...form.getInputProps('name')}
        />
        <Group grow align="flex-start">
          <NumberInput
            label="Quantity"
            min={0}
            decimalScale={3}
            allowNegative={false}
            key={form.key('quantity')}
            {...form.getInputProps('quantity')}
          />
          <Select
            label="Unit"
            data={unitOptions}
            allowDeselect={false}
            key={form.key('unit')}
            {...form.getInputProps('unit')}
          />
        </Group>
        <Select
          label="Category"
          data={categoryOptions}
          allowDeselect={false}
          key={form.key('category')}
          {...form.getInputProps('category')}
        />
        <Select
          label="Shop"
          placeholder="Any shop"
          data={merchantOptions}
          clearable
          key={form.key('merchant')}
          {...form.getInputProps('merchant')}
        />
        {withNotes && (
          <Textarea
            label="Notes"
            placeholder="Brand, size, …"
            autosize
            minRows={1}
            maxRows={4}
            key={form.key('notes')}
            {...form.getInputProps('notes')}
          />
        )}
        <Group justify="space-between">
          <Group>
            <Button type="submit" loading={form.submitting}>{submitLabel}</Button>
            <Button variant="default" onClick={onCancel}>Cancel</Button>
          </Group>
          {extraActions}
        </Group>
      </Stack>
    </form>
  )
}
