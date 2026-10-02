const TOKEN_KEY = "fc_token";

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

// Safely access Vite environment variables without needing a separate .d.ts file
const BASE_URL = ((import.meta as unknown) as { env?: { VITE_API_BASE_URL?: string } }).env?.VITE_API_BASE_URL || "";

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const isForm = init.body instanceof FormData;
  if (!isForm && !headers.has("Content-Type") && init.body) {
    headers.set("Content-Type", "application/json");
  }
  const token = getToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const url = path.startsWith("http") ? path : `\({BASE_URL}\){path}`;

  const res = await fetch(url, { ...init, headers });
  if (res.status === 204) return undefined as T;

  const text = await res.text();
  let data: any = {};
  try {
    data = text ? JSON.parse(text) : {};
  } catch {
    data = { error: text || "Server returned an invalid response" };
  }

  if (!res.ok) {
    if (res.status === 401 && getToken()) {
      setToken(null);
      throw new ApiError(401, "Your session expired. Please log in again.");
    }
    const message =
      typeof data.error === "string"
        ? data.error
        : typeof data.message === "string"
        ? data.message
        : "Request failed";
    throw new ApiError(res.status, message);
  }
  return data as T;
}

export function money(amount: number | null | undefined, currency = "INR") {
  const value = amount ?? 0;
  return new Intl.NumberFormat("en-IN", { style: "currency", currency }).format(value);
}