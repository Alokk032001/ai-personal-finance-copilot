export const CATEGORY_META: Record<string, { label: string; color: string }> = {
  GROCERIES: { label: "Groceries", color: "#34d399" },
  FOOD_DINING: { label: "Food & dining", color: "#f59e0b" },
  TRANSPORT: { label: "Transport", color: "#38bdf8" },
  SHOPPING: { label: "Shopping", color: "#a78bfa" },
  UTILITIES: { label: "Utilities", color: "#22d3ee" },
  RENT: { label: "Rent", color: "#fb7185" },
  HEALTHCARE: { label: "Healthcare", color: "#4ade80" },
  ENTERTAINMENT: { label: "Entertainment", color: "#f472b6" },
  EDUCATION: { label: "Education", color: "#60a5fa" },
  TRAVEL: { label: "Travel", color: "#c084fc" },
  PERSONAL_CARE: { label: "Personal care", color: "#fbbf24" },
  INSURANCE: { label: "Insurance", color: "#94a3b8" },
  INVESTMENT: { label: "Investment", color: "#2dd4bf" },
  FEES_CHARGES: { label: "Fees & charges", color: "#f97316" },
  OTHER: { label: "Other", color: "#64748b" },
};

export const CATEGORY_KEYS = Object.keys(CATEGORY_META);
