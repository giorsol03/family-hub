import { describe, expect, it } from 'vitest'
import { toFormErrors } from '../../api/client'
import { formatMinutes, splitSteps } from './steps'

describe('splitSteps', () => {
  it('makes one step per line and strips existing numbering or bullets', () => {
    expect(splitSteps('1. Mix flour and milk\n\n2) Rest 10 min\r\n- Fry\n • Serve ')).toEqual([
      'Mix flour and milk', 'Rest 10 min', 'Fry', 'Serve',
    ])
  })

  it('handles empty instructions', () => {
    expect(splitSteps(null)).toEqual([])
    expect(splitSteps('  \n ')).toEqual([])
  })

  it('keeps numbers that are part of the text', () => {
    expect(splitSteps('200 g sugar in')).toEqual(['200 g sugar in'])
  })
})

describe('formatMinutes', () => {
  it('formats durations', () => {
    expect(formatMinutes(45)).toBe('45 min')
    expect(formatMinutes(60)).toBe('1 h')
    expect(formatMinutes(75)).toBe('1 h 15 min')
  })
})

describe('toFormErrors', () => {
  it('converts server list paths to form paths', () => {
    expect(toFormErrors({ 'ingredients[0].name': 'Required', title: 'Required' })).toEqual({
      'ingredients.0.name': 'Required', title: 'Required',
    })
  })
})
