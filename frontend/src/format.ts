export const formatMoney = (value: number | null) => value == null ? '—' : new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(value);
export const formatDate = (value: string) => new Intl.DateTimeFormat('ru-RU', { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date(value));
export const initials = (name: string) => name.split(' ').map((part) => part[0]).slice(0, 2).join('').toUpperCase();
export const compactMoney = (value: number) => `$${new Intl.NumberFormat('ru-RU', { notation: 'compact', maximumFractionDigits: 1 }).format(value)}`;
