export type User = {
  id: string;
  email: string;
  fullName: string;
  currency: string;
};

export type AuthResponse = {
  token: string;
  expiresInSeconds: number;
  user: User;
};

export type BillStatus = "DRAFT" | "CONFIRMED";

export type ExpenseCategory =
  | "GROCERIES"
  | "FOOD_DINING"
  | "TRANSPORT"
  | "SHOPPING"
  | "UTILITIES"
  | "RENT"
  | "HEALTHCARE"
  | "ENTERTAINMENT"
  | "EDUCATION"
  | "TRAVEL"
  | "PERSONAL_CARE"
  | "INSURANCE"
  | "INVESTMENT"
  | "FEES_CHARGES"
  | "OTHER";

export type BillItemPayload = {
  name: string;
  quantity: number | null;
  amount: number | null;
  category: ExpenseCategory | null;
};

export type BillResponse = {
  id: string;
  status: BillStatus;
  merchantName: string | null;
  invoiceNumber: string | null;
  billDate: string | null;
  totalAmount: number | null;
  currency: string;
  category: ExpenseCategory;
  categoryConfidence: number | null;
  paymentMethod: string | null;
  items: BillItemPayload[];
  fileName: string | null;
  warnings: string[];
  aiProvider: string | null;
  aiModel: string | null;
  aiLatencyMs: number | null;
  userEdited: boolean;
  createdAt: string;
  confirmedAt: string | null;
};

export type BillUpdateRequest = {
  merchantName?: string | null;
  invoiceNumber?: string | null;
  billDate?: string | null;
  totalAmount?: number | null;
  currency?: string | null;
  category?: ExpenseCategory | null;
  paymentMethod?: string | null;
  items?: BillItemPayload[] | null;
};

export type CategorySpend = {
  category: string;
  total: number;
  billCount: number;
  share: number;
};

export type MonthlySpend = {
  month: string;
  total: number;
  billCount: number;
};

export type DashboardSummary = {
  totalSpend: number;
  thisMonthSpend: number;
  confirmedBillCount: number;
  pendingReviewCount: number;
  topCategory: string | null;
  thisMonthByCategory: CategorySpend[];
  recentMonths: MonthlySpend[];
  currency: string;
};

export type SpringPage<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};
