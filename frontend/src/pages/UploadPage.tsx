import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import ReviewCard from "../components/ReviewCard";
import type { BillResponse, BillUpdateRequest } from "../api/types";

export default function UploadPage() {
  const navigate = useNavigate();
  const [reading, setReading] = useState(false);
  const [bill, setBill] = useState<BillResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function upload(file: File) {
    setError(null);
    setReading(true);
    const body = new FormData();
    body.append("file", file);
    try {
      const created = await api<BillResponse>("/api/bills", { method: "POST", body });
      setBill(created);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Upload failed");
    } finally {
      setReading(false);
    }
  }

  return (
    <div>
      <h1>Upload bill</h1>
      {error && <p className="error">{error}</p>}
      {!bill && (
        <label className="dropzone">
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp,application/pdf"
            hidden
            onChange={(e) => {
              const file = e.target.files?.[0];
              if (file) void upload(file);
            }}
          />
          {reading ? "Reading your bill..." : "Drop a JPEG, PNG, WEBP, or PDF (max 10 MB), or click to choose"}
        </label>
      )}
      {reading && <p className="muted">Reading your bill...</p>}
      {bill && (
        <ReviewCard
          bill={bill}
          onSave={async (payload: BillUpdateRequest) => {
            await api(`/api/bills/${bill.id}/confirm`, {
              method: "POST",
              body: JSON.stringify(payload),
            });
            navigate("/dashboard");
          }}
          onDiscard={async () => {
            await api(`/api/bills/${bill.id}`, { method: "DELETE" });
            navigate("/bills");
          }}
        />
      )}
    </div>
  );
}
