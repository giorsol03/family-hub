/**
 * Splits free-text instructions into steps: one per non-empty line, with any leading
 * "1." / "2)" / "-" / "•" removed (the UI numbers them itself).
 */
export function splitSteps(instructions: string | null): string[] {
  if (!instructions) return []
  return instructions
    .split(/\r?\n/)
    .map((line) => line.trim().replace(/^(\d+[.)]|[-*•])\s*/, '').trim())
    .filter((line) => line.length > 0)
}

/** "1 h 15 min", "45 min" */
export function formatMinutes(minutes: number): string {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (h === 0) return `${m} min`
  return m === 0 ? `${h} h` : `${h} h ${m} min`
}
