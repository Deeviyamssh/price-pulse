import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { ApiError } from '../api/client';
import {
  addProduct,
  deleteProduct,
  listProducts,
  pauseProduct,
  removeAlert,
  resumeProduct,
  setAlert,
  type Product,
} from '../api/products';
import { useAuth } from '../auth/AuthContext';
import { AddProductForm } from '../components/AddProductForm';
import { ProductCard } from '../components/ProductCard';


export function DashboardPage() {
  const { currentUser, logout } = useAuth();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { data: products = [], isLoading, error } = useQuery<Product[], Error>({
    queryKey: ['products'],
    queryFn: listProducts,
  });

  const [isAdding, setIsAdding] = useState(false);
  const [isAddFormOpen, setIsAddFormOpen] = useState(false);
  const [pendingActions, setPendingActions] = useState<Record<number, 'pause' | 'resume' | 'delete'>>({});
  const [actionMessages, setActionMessages] = useState<Record<number, string>>({});
  const [pendingAlerts, setPendingAlerts] = useState<Record<number, 'save' | 'remove'>>({});

  async function handleProductAction(product: Product, action: 'pause' | 'resume' | 'delete') {
    if (pendingActions[product.id]) return;
    if (action === 'delete' && !window.confirm(`Delete ${product.displayName}?`)) return;

    setPendingActions((pending) => ({ ...pending, [product.id]: action }));
    setActionMessages((messages) => ({ ...messages, [product.id]: '' }));

    try {
      if (action === 'delete') {
        await deleteProduct(product.id);
        // Optimistically remove the product from the cache
        queryClient.setQueryData<Product[]>(['products'], (old = []) =>
          old.filter((item) => item.id !== product.id)
        );
      } else {
        const updated = action === 'pause'
          ? await pauseProduct(product.id)
          : await resumeProduct(product.id);
        // Optimistically update the product in the cache
        queryClient.setQueryData<Product[]>(['products'], (old = []) =>
          old.map((item) => item.id === updated.id ? updated : item)
        );
      }
    } catch (cause) {
      if (cause instanceof ApiError && cause.status === 409) {
        const state = action === 'pause' ? 'already paused' : 'already active';
        setActionMessages((messages) => ({ ...messages, [product.id]: `Product is ${state}.` }));
      } else if (cause instanceof Error) {
        setActionMessages((messages) => ({ ...messages, [product.id]: cause.message }));
      } else {
        setActionMessages((messages) => ({ ...messages, [product.id]: 'Unable to update this product.' }));
      }
    } finally {
      setPendingActions((pending) => {
        const next = { ...pending };
        delete next[product.id];
        return next;
      });
    }
  }

  async function handleAlertAction(product: Product, action: 'save' | 'remove', targetPrice?: number) {
    // Prevent duplicate actions
    const pendingAlertAction = pendingAlerts[product.id];
    if (pendingAlertAction) return;

    // Set pending state
    setPendingAlerts((pending) => ({ ...pending, [product.id]: action }));
    setActionMessages((messages) => ({ ...messages, [product.id]: '' }));

    try {
      let alert;
      if (action === 'save') {
        // Use the provided targetPrice or fall back to product's current targetPrice
        const priceToUse = targetPrice !== undefined ? targetPrice : (product.targetPrice ?? 0);
        alert = await setAlert(product.id, priceToUse);
      } else {
        alert = await removeAlert(product.id);
      }

      // Optimistically update the product in the cache
      queryClient.setQueryData<Product[]>(['products'], (old = []) =>
        old.map((item) =>
          item.id === product.id
            ? { ...item, targetPrice: alert.targetPrice, alertActive: alert.isActive }
            : item
        )
      );
    } catch (cause) {
      setActionMessages((messages) => ({
        ...messages,
        [product.id]: cause instanceof Error ? cause.message : 'Unable to update the price alert.',
      }));
    } finally {
      // Clear pending state
      setPendingAlerts((pending) => {
        const next = { ...pending };
        delete next[product.id];
        return next;
      });
    }
  }

  async function handleAddProduct(url: string, displayName: string) {
    setIsAdding(true);

    try {
      const product = await addProduct({ url, displayName });
      // Optimistically add the product to the cache
      queryClient.setQueryData<Product[]>(['products'], (old = []) =>
        [product, ...old]
      );
      setIsAddFormOpen(false);
    } catch (cause) {
      if (cause instanceof Error) {
        // AddProductForm will display its own error
        throw cause;
      } else {
        throw new Error('Unable to add this product.');
      }
    } finally {
      setIsAdding(false);
    }
  }

  return (
    <main className="min-h-screen flex flex-column">
      <header className="bg-white shadow-md px-6 py-4">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-bold text-primary">PricePulse dashboard</h1>
            <p className="text-sm text-muted">{currentUser?.email}</p>
          </div>
          <button type="button" onClick={() => {
            logout().finally(() => {
              navigate('/login', { replace: true });
            });
          }} className="btn btn-outline">
            Log out
          </button>
        </div>
      </header>

      <section className="flex-1 overflow-y-auto px-6 py-4">
        <div className="space-y-6">
          <div aria-labelledby="add-product-heading">
            <h2 id="add-product-heading" className="text-xl font-bold text-primary mb-4">
              Add a product
            </h2>
            <button
              type="button"
              onClick={() => setIsAddFormOpen(true)}
              disabled={isAdding}
              className="btn btn-primary"
            >
              {isAdding ? 'Adding…' : 'Add product'}
            </button>
          </div>

          <div aria-labelledby="products-heading">
            <h2 id="products-heading" className="text-xl font-bold text-primary mb-4">
              Your products
            </h2>
            {isLoading && (
              <p className="text-center py-8">Loading your products…</p>
            )}
            {!isLoading && error && (
              <div role="alert" className="alert alert-error text-center py-8">
                <p>{error.message}</p>
                <button type="button" onClick={() => window.location.reload()} className="btn btn-primary mt-4">
                  Try again
                </button>
              </div>
            )}
            {!isLoading && !error && products.length === 0 && (
              <p className="text-center py-8 text-muted">You are not monitoring any products yet.</p>
            )}
            {!isLoading && !error && products.length > 0 && (
              <ul className="space-y-4">
                {products.map((product: Product) => {
                  // Determine pending state for this product
                  const pendingAction =
                    pendingActions[product.id] ||
                    pendingAlerts[product.id] ||
                    null;
                  const isPending = !!pendingAction;

                  return (
                    <li key={product.id} className="border rounded-lg p-4">
                      <ProductCard
                        product={product}
                        isPending={isPending}
                        pendingAction={pendingAction}
                        onPauseResume={() => handleProductAction(product, product.status === 'PAUSED' ? 'resume' : 'pause')}
                        onDelete={() => handleProductAction(product, 'delete')}
                        onSetAlert={(targetPrice) => handleAlertAction(product, 'save', targetPrice)}
                        onRemoveAlert={() => handleAlertAction(product, 'remove')}
                        actionMessage={actionMessages[product.id]}
                      />
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </div>
      </section>

      {isAddFormOpen && (
        <AddProductForm
          onAdd={handleAddProduct}
          onClose={() => setIsAddFormOpen(false)}
        />
      )}
    </main>
  );
}