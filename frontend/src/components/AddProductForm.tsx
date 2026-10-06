import { FormEvent, useState } from 'react';

interface AddProductFormProps {
  onAdd: (url: string, displayName: string) => Promise<void>;
  onClose: () => void;
}

export function AddProductForm({ onAdd, onClose }: AddProductFormProps) {
  const [url, setUrl] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [isAdding, setIsAdding] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsAdding(true);
    setError(null);

    try {
      await onAdd(url, displayName);
      setUrl('');
      setDisplayName('');
      onClose();
    } catch (cause) {
      if (cause instanceof Error) {
        setError(cause.message);
      } else {
        setError('Unable to add this product.');
      }
    } finally {
      setIsAdding(false);
    }
  }

  return (
    <div className="fixed inset-0 flex items-center justify-center z-30">
      <div className="card w-full max-w-md mx-4">
        <header className="card-header">
          <h2 className="text-xl font-bold text-primary mb-0">Add a product</h2>
        </header>
        <div className="card-body">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="product-url" className="form-label">
                Product URL
              </label>
              <input
                id="product-url"
                type="url"
                className="form-input w-full"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                placeholder="https://example.com/product"
                required
              />
            </div>
            <div>
              <label htmlFor="product-display-name" className="form-label">
                Display name
              </label>
              <input
                id="product-display-name"
                type="text"
                className="form-input w-full"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                placeholder="Example product"
                maxLength={255}
                required
              />
            </div>
            {error && (
              <p className="form-error" role="alert">
                {error}
              </p>
            )}
            <div className="space-y-3">
              <button
                type="submit"
                disabled={isAdding}
                className="btn btn-primary w-full"
              >
                {isAdding ? 'Adding…' : 'Add product'}
              </button>
              <button
                type="button"
                onClick={onClose}
                className="btn btn-outline w-full"
              >
                Cancel
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}