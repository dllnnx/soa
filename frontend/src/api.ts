type ApiErrorBody = { message?: string; details?: Array<{ field: string; message: string }> };

export class ApiRequestError extends Error {
  constructor(message: string, readonly status: number) { super(message); }
}

export const api = async <T,>(path: string, init: RequestInit = {}): Promise<T> => {
  const response = await fetch(path, {
    ...init,
    headers: { ...(init.body ? { 'Content-Type': 'application/json' } : {}), ...init.headers },
  });
  const text = await response.text();
  const body = text ? JSON.parse(text) as T | ApiErrorBody : undefined;
  if (!response.ok) {
    const error = (body ?? {}) as ApiErrorBody;
    const details = error.details?.map((item) => `${item.field}: ${item.message}`).join('; ');
    throw new ApiRequestError([error.message || `HTTP ${response.status}`, details].filter(Boolean).join(' — '), response.status);
  }
  return body as T;
};
