import { apiFetch } from './client';

export type Product = {
  id: number;
  displayName: string;
  url: string;
  status: 'ACTIVE' | 'PAUSED' | string;
  createdAt: string;
  currentPrice: number | null;
  previousPrice: number | null;
  lowestRecordedPrice: number | null;
  targetPrice: number | null;
  lastCheckedAt: string | null;
  lastCheckStatus: 'SUCCESS' | 'FAILED' | string | null;
  errorMessage: string | null;
  alertActive: boolean;
};

export type AddProductRequest = {
  url: string;
  displayName: string;
};

export type PriceRecord = {
  id: number;
  price: number | null;
  currency: string | null;
  checkedAt: string;
  checkStatus: 'SUCCESS' | 'FAILED' | string;
  errorMessage: string | null;
};

export type Alert = {
  id: number | null;
  productId: number;
  targetPrice: number | null;
  isActive: boolean;
  lastNotifiedAt: string | null;
};

export function listProducts() {
  return apiFetch<Product[]>('/api/products');
}

export function getProduct(productId: number) {
  return apiFetch<Product>(`/api/products/${productId}`);
}

export function getPriceHistory(productId: number) {
  return apiFetch<PriceRecord[]>(`/api/products/${productId}/prices`);
}

export function addProduct(request: AddProductRequest) {
  return apiFetch<Product>('/api/products', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function pauseProduct(productId: number) {
  return apiFetch<Product>(`/api/products/${productId}/pause`, { method: 'PATCH' });
}

export function resumeProduct(productId: number) {
  return apiFetch<Product>(`/api/products/${productId}/resume`, { method: 'PATCH' });
}

export function deleteProduct(productId: number) {
  return apiFetch<void>(`/api/products/${productId}`, { method: 'DELETE' });
}

export function setAlert(productId: number, targetPrice: number) {
  return apiFetch<Alert>(`/api/products/${productId}/alert`, {
    method: 'PUT',
    body: JSON.stringify({ targetPrice }),
  });
}

export function removeAlert(productId: number) {
  return apiFetch<Alert>(`/api/products/${productId}/alert`, { method: 'DELETE' });
}
