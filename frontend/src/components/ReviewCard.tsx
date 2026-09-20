import { useState } from "react";
import { CATEGORY_KEYS, CATEGORY_META } from "../api/categories";
import { money } from "../api/client";
import type { BillItemPayload, BillResponse, BillUpdateRequest, ExpenseCategory } from "../api/types";

type Props = {
  bill: BillResponse;
  onSave: (body: BillUpdateRequest) => Promise<void>;
  onDiscard: () => Promise<void>;
};

export default function ReviewCard({ bill, onSave, onDiscard }: Props) {
  const [editing, setEditing] = useState(bill.status === "DRAFT");
  const [merchantName, setMerchantName] = useState(bill.merchantName ?? "");
  const [billDate, setBillDate] = useState(bill.billDate ?? "");
  const [totalAmount, setTotalAmount] = useState(String(bill.totalAmount ?? ""));
  const [category, setCategory] = useState<ExpenseCategory>(bill.category);
  const [paymentMethod, setPaymentMethod] = useState(bill.paymentMethod ?? "");
  const [items, setItems] = useState<BillItemPayload[]>(bill.items ?? []);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const confidence = Math.round((bill.categoryConfidence ?? 0) * 100);
  const low = confidence < 60;

  async function confirm() {
    setBusy(true);
    setError(null);
    try {
      await onSave({
        merchantName,
        billDate: billDate || null,
        totalAmount: Number(totalAmount),
        category,
        paymentMethod: paymentMethod || null,
        items,
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Save failed");
    } finally {
      setBusy(false);
    }
  }

  async function discard() {
    setBusy(true);
    setError(null);
    try {
      await onDiscard();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Discard failed");
      setBusy(false);
    }
  }

  return (
    <section className="card">
      <div className="row" style={{ justifyContent: "space-between" }}>
        <h2 style={{ margin: 0 }}>We analyzed your bill</h2>
        <span className={`badge ${low ? "warn" : "ok"}`}>
          {confidence}% confidence
        </span>
      </div>
      {bill.warnings?.length > 0 && (
        <ul className="muted">
          {bill.warnings.map((w) => (
            <li key={w}>{w}</li>
          ))}
        </ul>
      )}
      {error && <p className="error">{error}</p>}

      <div className="grid two" style={{ marginTop: "1rem" }}>
        <div>
          <label>Merchant</label>
          <input value={merchantName} disabled={!editing} onChange={(e) => setMerchantName(e.target.value)} />
          <label>Date</label>
          <input type="date" value={billDate} disabled={!editing} onChange={(e) => setBillDate(e.target.value)} />
          <label>Total</label>
          <input value={totalAmount} disabled={!editing} onChange={(e) => setTotalAmount(e.target.value)} />
        </div>
        <div>
          <label>Category</label>
          <select value={category} disabled={!editing} onChange={(e) => setCategory(e.target.value as ExpenseCategory)}>
            {CATEGORY_KEYS.map((key) => (
              <option key={key} value={key}>{CATEGORY_META[key].label}</option>
            ))}
          </select>
          <label>Payment</label>
          <input value={paymentMethod} disabled={!editing} onChange={(e) => setPaymentMethod(e.target.value)} />
          <p className="muted">Shown total: {money(Number(totalAmount) || 0, bill.currency)}</p>
        </div>
      </div>

      <h3>Line items</h3>
      {items.map((item, idx) => (
        <div className="row" key={`${item.name}-${idx}`}>
          <input
            value={item.name}
            disabled={!editing}
            onChange={(e) => {
              const next = [...items];
              next[idx] = { ...item, name: e.target.value };
              setItems(next);
            }}
          />
          <input
            value={item.amount ?? ""}
            disabled={!editing}
            onChange={(e) => {
              const next = [...items];
              next[idx] = { ...item, amount: Number(e.target.value) };
              setItems(next);
            }}
          />
        </div>
      ))}

      {bill.status === "DRAFT" && (
        <div className="row" style={{ marginTop: "1rem" }}>
          <button className="btn secondary" type="button" onClick={() => setEditing((v) => !v)}>
            {editing ? "Lock fields" : "Edit"}
          </button>
          <button className="btn" type="button" disabled={busy} onClick={confirm}>Save</button>
          <button className="btn danger" type="button" disabled={busy} onClick={discard}>Discard</button>
        </div>
      )}
    </section>
  );
}
