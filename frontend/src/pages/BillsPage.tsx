import { useEffect, useState } from "react";
import { api, money } from "../api/client";
import { CATEGORY_META } from "../api/categories";
import type { BillResponse, BillStatus, SpringPage } from "../api/types";

const FILTERS: { id: "ALL" | BillStatus; label: string }[] = [
  { id: "ALL", label: "ALL" },
  { id: "DRAFT", label: "Needs review" },
  { id: "CONFIRMED", label: "Saved" },
];

export default function BillsPage() {
  const [filter, setFilter] = useState<"ALL" | BillStatus>("ALL");
  const [page, setPage] = useState<SpringPage<BillResponse> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const qs = filter === "ALL" ? "" : `?status=${filter}`;
    api<SpringPage<BillResponse>>(`/api/bills${qs}`)
      .then(setPage)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load"));
  }, [filter]);

  return (
    <div>
      <h1>Bills</h1>
      <div className="row filters" style={{ marginBottom: "1rem" }}>
        {FILTERS.map((f) => (
          <button key={f.id} className={`btn ${filter === f.id ? "on" : "secondary"}`} type="button" onClick={() => setFilter(f.id)}>
            {f.label}
          </button>
        ))}
      </div>
      {error && <p className="error">{error}</p>}
      <table className="table">
        <thead>
          <tr>
            <th>Merchant</th>
            <th>Date</th>
            <th>Category</th>
            <th>Total</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {page?.content.map((bill) => (
            <tr key={bill.id}>
              <td>{bill.merchantName ?? "—"}</td>
              <td>{bill.billDate ?? "—"}</td>
              <td>{CATEGORY_META[bill.category]?.label ?? bill.category}</td>
              <td>{money(bill.totalAmount, bill.currency)}</td>
              <td>
                <span className={`badge ${bill.status === "DRAFT" ? "draft" : "saved"}`}>
                  {bill.status === "DRAFT" ? "Needs review" : "Saved"}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {page && page.content.length === 0 && <p className="muted">No bills in this filter.</p>}
    </div>
  );
}
