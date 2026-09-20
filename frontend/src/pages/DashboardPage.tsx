import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Bar, BarChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import { CATEGORY_META } from "../api/categories";
import { api, money } from "../api/client";
import type { DashboardSummary } from "../api/types";

export default function DashboardPage() {
  const [data, setData] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<DashboardSummary>("/api/analytics/summary")
      .then(setData)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load"));
  }, []);

  if (error) return <p className="error">{error}</p>;
  if (!data) return <p className="muted">Loading...</p>;

  const empty = data.confirmedBillCount === 0;

  return (
    <div>
      <h1>Dashboard</h1>
      {data.pendingReviewCount > 0 && (
        <div className="banner">
          {data.pendingReviewCount} bill{data.pendingReviewCount === 1 ? "" : "s"} need review.{" "}
          <Link to="/bills">Open bills</Link>
        </div>
      )}
      {empty && (
        <div className="card" style={{ marginBottom: "1rem" }}>
          <p>No confirmed spend yet. Upload a bill — drafts never count until you save them.</p>
          <Link className="btn" to="/upload">Upload a bill</Link>
        </div>
      )}
      <div className="grid stats">
        <div className="card">
          <div className="muted">Total spend</div>
          <p className="stat-value">{money(data.totalSpend, data.currency)}</p>
        </div>
        <div className="card">
          <div className="muted">This month</div>
          <p className="stat-value">{money(data.thisMonthSpend, data.currency)}</p>
        </div>
        <div className="card">
          <div className="muted">Confirmed bills</div>
          <p className="stat-value">{data.confirmedBillCount}</p>
        </div>
        <div className="card">
          <div className="muted">Top category</div>
          <p className="stat-value">{data.topCategory ? (CATEGORY_META[data.topCategory]?.label ?? data.topCategory) : "—"}</p>
        </div>
      </div>

      <div className="grid two" style={{ marginTop: "1rem" }}>
        <div className="card">
          <h2>Last 6 months</h2>
          <div style={{ height: 260 }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={data.recentMonths}>
                <XAxis dataKey="month" stroke="#a1a1aa" />
                <YAxis stroke="#a1a1aa" />
                <Tooltip />
                <Bar dataKey="total" fill="#6366f1" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
        <div className="card">
          <h2>This month by category</h2>
          {data.thisMonthByCategory.length === 0 && <p className="muted">Nothing confirmed this month.</p>}
          {data.thisMonthByCategory.map((row) => {
            const meta = CATEGORY_META[row.category] ?? CATEGORY_META.OTHER;
            return (
              <div className="share-row" key={row.category}>
                <span style={{ width: 120 }}>{meta.label}</span>
                <div className="share-bar">
                  <span style={{ width: `${Math.round(row.share * 100)}%`, background: meta.color }} />
                </div>
                <span>{money(row.total, data.currency)}</span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
