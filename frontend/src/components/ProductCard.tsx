import { Link } from 'react-router-dom';
import { FormEvent, useState, useEffect } from 'react';
import { formatPrice, formatDate } from '../utils/formatters';

interface ProductCardProps {
  product: {
    id: number;
    displayName: string;
    url: string;
    status: 'ACTIVE' | 'PAUSED' | string;
    currentPrice: number | null;
    previousPrice: number | null;
    lowestRecordedPrice: number | null;
    targetPrice: number | null;
    lastCheckedAt: string | null;
    lastCheckStatus: 'SUCCESS' | 'FAILED' | string | null;
    errorMessage: string | null;
    alertActive: boolean;
  };
  isPending: boolean;
  pendingAction: 'pause' | 'resume' | 'delete' | 'save' | 'remove' | null;
  onPauseResume: (productId: number) => void;
  onDelete: (productId: number) => void;
  onSetAlert: (productId: number, targetPrice: number) => void;
  onRemoveAlert: (productId: number) => void;
  actionMessage?: string;
}

export function ProductCard({
  product,
  isPending,
  pendingAction,
  onPauseResume,
  onDelete,
  onSetAlert,
  onRemoveAlert,
  actionMessage,
}: ProductCardProps) {
  const [alertTarget, setAlertTarget] = useState(product.targetPrice?.toFixed(2) ?? '');

  // Sync input with prop changes (e.g., after optimistic update from parent)
  useEffect(() => {
    setAlertTarget(product.targetPrice?.toFixed(2) ?? '');
  }, [product.targetPrice]);

  const handleAlertSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const price = Number(alertTarget);
    if (price > 0) {
      onSetAlert(product.id, price);
    }
  };

  return (
    <article className="card">
      <header className="card-header">
        <h3 className="text-lg font-bold text-primary mb-0">
          <Link to={`/products/${product.id}`} className="hover:underline">{product.displayName}</Link>
        </h3>
      </header>
      <div className="card-body">
        <p>
          <a href={product.url} target="_blank" rel="noreferrer" className="text-primary hover:underline">
            Open product page
          </a>
        </p>
        <p>Status: <span className="font-medium">{product.status}</span></p>
        {actionMessage && (
          <p className="form-error" role="alert">
            {actionMessage}
          </p>
        )}
        <p>Current price: <span className="font-medium">{formatPrice(product.currentPrice)}</span></p>
        <p>Previous price: <span className="font-medium">{formatPrice(product.previousPrice)}</span></p>
        <p>Lowest recorded: <span className="font-medium">{formatPrice(product.lowestRecordedPrice)}</span></p>
        <p>
          Last checked: {formatDate(product.lastCheckedAt)} {product.lastCheckStatus === 'FAILED' && (
            <span role="alert" className="text-danger ml-2"> — {product.errorMessage}</span>
          )}
        </p>
        <p>
          Alert:{' '}
          {product.alertActive ? (
            `Active at ${formatPrice(product.targetPrice)}`
          ) : product.targetPrice !== null ? (
            `Inactive at ${formatPrice(product.targetPrice)}`
          ) : (
            'Not set'
          )}
        </p>

        <div className="space-y-3">
          <button
            type="button"
            disabled={isPending || pendingAction === 'pause' || pendingAction === 'resume'}
            onClick={() => onPauseResume(product.id)}
            className="btn btn-outline w-full"
          >
            {isPending && (pendingAction === 'pause' || pendingAction === 'resume') ? (
              product.status === 'PAUSED' ? 'Resuming…' : 'Pausing…'
            ) : (
              product.status === 'PAUSED' ? 'Resume' : 'Pause'
            )}
          </button>
          <button
            type="button"
            disabled={isPending || pendingAction === 'delete'}
            onClick={() => onDelete(product.id)}
            className="btn btn-outline w-full"
          >
            {isPending && pendingAction === 'delete' ? 'Deleting…' : 'Delete'}
          </button>
        </div>

        <div>
          <form onSubmit={handleAlertSubmit} className="space-y-3">
            <label htmlFor={`alert-target-${product.id}`} className="form-label">
              Target price
            </label>
            <input
              id={`alert-target-${product.id}`}
              type="number"
              min="0.01"
              step="0.01"
              className="form-input w-full"
              value={alertTarget}
              onChange={(e) => setAlertTarget(e.target.value)}
              placeholder="75.00"
            />
            <div className="space-y-2">
              <button type="submit" disabled={isPending || pendingAction === 'save'} className="btn btn-outline w-full">
                {isPending && pendingAction === 'save' ? 'Saving…' : product.targetPrice === null ? 'Set alert' : 'Update alert'}
              </button>
              {product.targetPrice !== null && (
                <button
                  type="button"
                  disabled={isPending || pendingAction === 'remove'}
                  onClick={() => onRemoveAlert(product.id)}
                  className="btn btn-outline w-full"
                >
                  {isPending && pendingAction === 'remove' ? 'Removing…' : 'Remove alert'}
                </button>
              )}
            </div>
          </form>
        </div>
      </div>
    </article>
  );
}