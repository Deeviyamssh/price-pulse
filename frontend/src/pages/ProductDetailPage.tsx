import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getPriceHistory, getProduct, type PriceRecord } from '../api/products';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ReferenceDot } from 'recharts';
import { AlertPanel } from '../components/AlertPanel';

function formatPrice(price: number | null, currency: string | null = null) {
  if (price === null) return '—';
  return `${currency ? `${currency} ` : ''}${price.toFixed(2)}`;
}

function formatDate(value: string) {
  return new Date(value).toLocaleString();
}

function PriceChart({ records }: { records: PriceRecord[] }) {
  const successful = records.filter((record) => record.checkStatus === 'SUCCESS' && record.price !== null);
  if (successful.length < 2) return <p className="text-center py-4 text-muted">Not enough data to show a chart yet.</p>;

  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={successful}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis
          dataKey="checkedAt"
          tickFormatter={(value) => new Date(value).toLocaleString()}
        />
        <YAxis />
        <Tooltip formatter={(value) => `$${value}`} />
        <Legend />
        <Line
          type="monotone"
          dataKey="price"
          stroke="#8884d8"
          strokeWidth={2}
        />
        {/* Failure markers for failed checks */}
        {records.map((record) => (
          record.checkStatus === 'FAILED' && (
            <ReferenceDot
              key={record.id}
              x={record.checkedAt}
              y={0}
              yAxisId={0}
              stroke="#ff0000"
              r={4}
            />
          ))
        )}
      </LineChart>
    </ResponsiveContainer>
  );
}

export function ProductDetailPage() {
  const { id } = useParams();
  const productId = Number(id);

  const { data: product, isLoading: productLoading, error: productError } = useQuery({
    queryKey: ['product', productId],
    queryFn: () => getProduct(productId),
    enabled: !!productId && Number.isInteger(productId) && productId > 0,
  });

  const { data: records, isLoading: recordsLoading, error: recordsError } = useQuery({
    queryKey: ['priceHistory', productId],
    queryFn: () => getPriceHistory(productId),
    enabled: !!productId && Number.isInteger(productId) && productId > 0,
  });

  const isLoading = productLoading || recordsLoading;
  const error = productError || recordsError;
  const successfulCount = product && records
    ? records.filter((record) => record.checkStatus === 'SUCCESS').length
    : 0;

  if (isLoading) return <main className="min-h-screen flex flex-col items-center justify-center py-12"><p>Loading product details…</p></main>;
  if (error || !product) {
    const errorMessage = error instanceof Error ? error.message : String(error ?? 'Product not found.');
    return (
      <main className="min-h-screen flex flex-col items-center justify-center py-12">
        <p role="alert" className="text-danger text-center">{errorMessage}</p>
        <Link to="/" className="btn btn-primary mt-4">Back to dashboard</Link>
      </main>
    );
  }

  return (
    <main className="min-h-screen flex-1 flex-column">
      <div className="container px-6 py-4">
        <div className="flex justify-between items-start mb-4">
          <p>
            <Link to="/" className="text-primary hover:underline">
              ← Back to dashboard
            </Link>
          </p>
          <h1 className="text-2xl font-bold text-primary">{product.displayName}</h1>
        </div>

        <div className="space-y-4 mb-6">
          <p><a href={product.url} target="_blank" rel="noreferrer" className="text-primary hover:underline">Open product page</a></p>
          <p>Status: <span className="font-medium">{product.status}</span></p>
          <p>Current price: <span className="font-medium">{formatPrice(product.currentPrice)}</span></p>
          <p>Previous price: <span className="font-medium">{formatPrice(product.previousPrice)}</span></p>
          <p>Lowest recorded: <span className="font-medium">{formatPrice(product.lowestRecordedPrice)}</span></p>
        </div>

        <AlertPanel
          productId={product.id}
          targetPrice={product.targetPrice}
          isActive={product.alertActive}
        />

        <section aria-labelledby="chart-heading" className="mb-6">
          <h2 id="chart-heading" className="text-xl font-bold text-primary mb-4">
            Price history
          </h2>
          <p className="text-sm text-muted mb-4">
            {successfulCount} successful checks plotted from {records?.length ?? 0} total records.
          </p>
          <PriceChart records={records ?? []} />
        </section>

        <section aria-labelledby="records-heading">
          <h2 id="records-heading" className="text-xl font-bold text-primary mb-4">
            All checks
          </h2>
          {(records?.length ?? 0) === 0 ? (
            <p className="text-center py-8 text-muted">No price checks recorded yet.</p>
          ) : (
            <table className="table w-full">
              <thead>
                <tr>
                  <th scope="col" className="text-left text-xs font-semibold text-muted uppercase">
                    Checked at
                  </th>
                  <th scope="col" className="text-left text-xs font-semibold text-muted uppercase">
                    Status
                  </th>
                  <th scope="col" className="text-left text-xs font-semibold text-muted uppercase">
                    Price
                  </th>
                  <th scope="col" className="text-left text-xs font-semibold text-muted uppercase">
                    Details
                  </th>
                </tr>
              </thead>
              <tbody>
                {records?.map((record) => (
                  <tr key={record.id} className="border-t hover:bg-background">
                    <td className="py-3 text-sm">{formatDate(record.checkedAt)}</td>
                    <td className="py-3 text-sm font-medium">
                      {record.checkStatus === 'SUCCESS' ? (
                        <span className="text-success">{record.checkStatus}</span>
                      ) : (
                        <span className="text-danger">{record.checkStatus}</span>
                      )}
                    </td>
                    <td className="py-3 text-sm">{formatPrice(record.price, record.currency)}</td>
                    <td className="py-3 text-sm text-muted">{record.errorMessage ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </div>
    </main>
  );
}