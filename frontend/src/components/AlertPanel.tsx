import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { removeAlert, setAlert, type Alert } from '../api/products';
import { formatPrice } from '../utils/formatters';

interface AlertPanelProps {
  productId: number;
  targetPrice: number | null;
  isActive: boolean;
}

export function AlertPanel({ productId, targetPrice, isActive }: AlertPanelProps) {
  const queryClient = useQueryClient();
  const [pendingAction, setPendingAction] = useState<'save' | 'remove' | null>(null);
  const [targetInput, setTargetInput] = useState(targetPrice?.toFixed(2) ?? '');
  const [error, setError] = useState<string | null>(null);

  const handleSave = async () => {
    const price = Number(targetInput);
    if (!Number.isFinite(price) || price <= 0) {
      setError('Enter a target price greater than zero.');
      return;
    }

    setPendingAction('save');
    setError(null);

    try {
      const alert: Alert = await setAlert(productId, price);
      // Optimistically update the product in the cache
      queryClient.setQueryData(['product', productId], (old: any) => ({
        ...old,
        targetPrice: alert.targetPrice,
        alertActive: alert.isActive,
      }));
      setTargetInput(alert.targetPrice?.toFixed(2) ?? '');
    } catch (cause) {
      if (cause instanceof Error) {
        setError(cause.message);
      } else {
        setError('Unable to update the price alert.');
      }
    } finally {
      setPendingAction(null);
    }
  };

  const handleRemove = async () => {
    setPendingAction('remove');
    setError(null);

    try {
      const alert: Alert = await removeAlert(productId);
      // Optimistically update the product in the cache
      queryClient.setQueryData(['product', productId], (old: any) => ({
        ...old,
        targetPrice: alert.targetPrice,
        alertActive: alert.isActive,
      }));
    } catch (cause) {
      if (cause instanceof Error) {
        setError(cause.message);
      } else {
        setError('Unable to remove the price alert.');
      }
    } finally {
      setPendingAction(null);
    }
  };

  return (
    <section className="card mb-6" aria-labelledby="alert-panel-heading">
      <header className="card-header">
        <h2 id="alert-panel-heading" className="text-xl font-bold text-primary mb-0">
          Price alert
        </h2>
      </header>
      <div className="card-body">
        <p className="text-sm text-muted mb-4">
          Current state:{' '}
          {isActive ? (
            `Active at ${formatPrice(targetPrice)}`
          ) : targetPrice !== null ? (
            `Inactive at ${formatPrice(targetPrice)}`
          ) : (
            'Not set'
          )}
        </p>

        {targetPrice !== null && (
          <form onSubmit={(e) => {
            e.preventDefault();
            handleSave();
          }} className="space-y-3">
            <label htmlFor={`alert-target-input-${productId}`} className="form-label">
              Target price
            </label>
            <input
              id={`alert-target-input-${productId}`}
              type="number"
              min="0.01"
              step="0.01"
              className="form-input w-full"
              value={targetInput}
              onChange={(e) => setTargetInput(e.target.value)}
              placeholder="75.00"
            />
            <button type="submit" disabled={pendingAction === 'save'} className="btn btn-primary w-full">
              {pendingAction === 'save' ? 'Saving…' : 'Save'}
            </button>
          </form>
        )}

        {targetPrice !== null && (
          <button
            type="button"
            onClick={handleRemove}
            disabled={pendingAction === 'remove'}
            className="btn btn-outline w-full mt-3"
          >
            {pendingAction === 'remove' ? 'Removing…' : 'Remove alert'}
          </button>
        )}

        {error && (
          <p role="alert" className="form-error mt-3">
            {error}
          </p>
        )}
      </div>
    </section>
  );
}