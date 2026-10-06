export function formatPrice(price: number | null, currency: string | null = null): string {
  if (price === null) return '—';
  return `${currency ? `${currency} ` : ''}${price.toFixed(2)}`;
}

export function formatDate(value: string | null): string {
  if (value === null) return 'Never';
  return new Date(value).toLocaleString();
}