export function toIsoString(date: Date | string | number): string {
  return new Date(date).toISOString();
}

export function addMilliseconds(
  date: Date,
  milliseconds: number
): Date {
  return new Date(date.getTime() + milliseconds);
}

export function subtractMilliseconds(
  date: Date,
  milliseconds: number
): Date {
  return new Date(date.getTime() - milliseconds);
}

export function isExpired(date: Date | string | number): boolean {
  return new Date(date).getTime() <= Date.now();
}

export function isValidDate(value: unknown): value is Date {
  return value instanceof Date && !Number.isNaN(value.getTime());
}
