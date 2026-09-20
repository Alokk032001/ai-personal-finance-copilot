package com.iitp.financecopilot.ai;

import com.iitp.financecopilot.domain.Bill;
import com.iitp.financecopilot.domain.BillItem;
import com.iitp.financecopilot.domain.BillStatus;
import com.iitp.financecopilot.domain.ExpenseCategory;
import com.iitp.financecopilot.domain.ExtractionMeta;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trust boundary. Failures become warnings — never abort an extraction we already paid for.
 */
@Component
public class BillNormalizer {

    private static final Pattern MONEY = Pattern.compile("-?\\d[\\d,]*+(?:\\.\\d+)?");
    private static final BigDecimal TWO_PERCENT = new BigDecimal("0.02");
    private static final List<DateTimeFormatter> DATES = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.SMART),
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.SMART),
            DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.SMART),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.SMART),
            DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.SMART),
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("uuuu/MM/dd")
    );

    public Bill normalizeInto(Bill bill, ExtractedBill extracted) {
        if (extracted == null) {
            extracted = new ExtractedBill();
        }
        List<String> warnings = new ArrayList<>();

        String merchant = blankToNull(extracted.getMerchantName());
        if (merchant == null) {
            warnings.add("Merchant name was missing.");
        }
        bill.setMerchantName(merchant);
        bill.setInvoiceNumber(blankToNull(extracted.getInvoiceNumber()));

        ParsedDate parsedDate = parseDate(extracted.getBillDate(), warnings);
        bill.setBillDate(parsedDate.date());

        ExpenseCategory category = ExpenseCategory.fromNullable(extracted.getCategory());
        String rawCategory = blankToNull(extracted.getCategory());
        if (rawCategory != null && category == ExpenseCategory.OTHER
                && !"OTHER".equalsIgnoreCase(rawCategory.trim().replace(' ', '_'))) {
            warnings.add("Unknown category '" + rawCategory + "' was mapped to OTHER.");
        }
        bill.setCategory(category);

        String currency = blankToNull(extracted.getCurrency());
        if (currency == null || currency.length() != 3) {
            currency = "INR";
        } else {
            currency = currency.toUpperCase(Locale.ROOT);
        }
        bill.setCurrency(currency);

        String pay = blankToNull(extracted.getPaymentMethod());
        bill.setPaymentMethod(pay == null ? null : pay.toUpperCase(Locale.ROOT));

        List<BillItem> items = normalizeItems(extracted.getItems(), category, warnings);
        bill.setItems(items);

        BigDecimal itemSum = items.stream()
                .map(BillItem::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = parseMoney(extracted.getTotalAmount());
        if (total == null) {
            if (itemSum.compareTo(BigDecimal.ZERO) > 0) {
                total = itemSum;
                warnings.add("Missing total; used the sum of line items.");
            } else {
                total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                warnings.add("Missing total amount.");
            }
        } else {
            if (itemSum.compareTo(BigDecimal.ZERO) > 0 && total.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal drift = itemSum.subtract(total).abs();
                if (drift.compareTo(total.multiply(TWO_PERCENT)) > 0) {
                    warnings.add("Line items do not sum to the printed total; keeping the printed total.");
                }
            }
        }
        bill.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));

        double confidence = parseConfidence(extracted.getCategoryConfidence(), warnings);
        bill.setCategoryConfidence(confidence);
        if (confidence < 0.60) {
            warnings.add("Low category confidence.");
        }

        bill.setStatus(BillStatus.DRAFT);

        ExtractionMeta meta = bill.getExtraction();
        if (meta == null) {
            meta = new ExtractionMeta();
            bill.setExtraction(meta);
        }
        List<String> existing = meta.getWarnings() == null ? new ArrayList<>() : new ArrayList<>(meta.getWarnings());
        existing.addAll(warnings);
        meta.setWarnings(existing);

        return bill;
    }

    private static List<BillItem> normalizeItems(
            List<ExtractedBill.ExtractedItem> extractedItems,
            ExpenseCategory billCategory,
            List<String> warnings) {
        List<BillItem> items = new ArrayList<>();
        if (extractedItems == null) {
            return items;
        }
        for (ExtractedBill.ExtractedItem raw : extractedItems) {
            if (raw == null) {
                continue;
            }
            String name = blankToNull(raw.getName());
            BigDecimal amount = parseMoney(raw.getAmount() == null ? null : String.valueOf(raw.getAmount()));
            if (name == null && amount == null) {
                continue;
            }
            BillItem item = new BillItem();
            item.setName(name == null ? "Unnamed item" : name);
            BigDecimal qty = parseMoney(raw.getQuantity() == null ? null : String.valueOf(raw.getQuantity()));
            item.setQuantity(qty == null ? BigDecimal.ONE : qty.abs());
            item.setAmount(amount == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount);
            String itemCat = blankToNull(raw.getCategory());
            if (itemCat == null) {
                item.setCategory(billCategory);
            } else {
                ExpenseCategory mapped = ExpenseCategory.fromNullable(itemCat);
                if (mapped == ExpenseCategory.OTHER && !"OTHER".equalsIgnoreCase(itemCat.trim().replace(' ', '_'))) {
                    warnings.add("Unknown item category '" + itemCat + "' was mapped to OTHER.");
                }
                item.setCategory(mapped);
            }
            items.add(item);
        }
        return items;
    }

    static BigDecimal parseMoney(String raw) {
        String cleaned = blankToNull(raw);
        if (cleaned == null) {
            return null;
        }
        Matcher matcher = MONEY.matcher(cleaned);
        if (!matcher.find()) {
            return null;
        }
        String token = matcher.group().replace(",", "");
        try {
            return new BigDecimal(token).abs().setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static double parseConfidence(Object raw, List<String> warnings) {
        if (raw == null || (raw instanceof String s && blankToNull(s) == null)) {
            warnings.add("Category confidence was missing; defaulted to 0.5.");
            return 0.5;
        }
        double value;
        try {
            if (raw instanceof Number n) {
                value = n.doubleValue();
            } else {
                value = Double.parseDouble(String.valueOf(raw).trim());
            }
        } catch (NumberFormatException ex) {
            warnings.add("Category confidence was missing; defaulted to 0.5.");
            return 0.5;
        }
        if (value > 1 && value <= 100) {
            value = value / 100.0;
        }
        if (value < 0) {
            value = 0;
        }
        if (value > 1) {
            value = 1;
        }
        return value;
    }

    private record ParsedDate(LocalDate date) {
    }

    private static ParsedDate parseDate(String raw, List<String> warnings) {
        String cleaned = blankToNull(raw);
        LocalDate today = LocalDate.now();
        if (cleaned == null) {
            warnings.add("Bill date was missing; defaulted to today.");
            return new ParsedDate(today);
        }
        LocalDate parsed = null;
        for (DateTimeFormatter formatter : DATES) {
            try {
                parsed = LocalDate.parse(cleaned, formatter);
                break;
            } catch (DateTimeParseException ignored) {
                // try next pattern
            }
        }
        if (parsed == null) {
            warnings.add("Could not parse bill date; defaulted to today.");
            return new ParsedDate(today);
        }
        if (parsed.isAfter(today.plusDays(1))) {
            warnings.add("Bill date is in the future.");
        }
        return new ParsedDate(parsed);
    }

    static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }
}
