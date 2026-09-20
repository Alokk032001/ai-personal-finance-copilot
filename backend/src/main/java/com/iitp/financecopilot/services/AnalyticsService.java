package com.iitp.financecopilot.services;

import com.iitp.financecopilot.domain.BillStatus;
import com.iitp.financecopilot.dto.analytics.CategorySpend;
import com.iitp.financecopilot.dto.analytics.DashboardSummary;
import com.iitp.financecopilot.dto.analytics.MonthlySpend;
import com.iitp.financecopilot.repositories.BillRepository;
import com.iitp.financecopilot.security.AuthUser;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final MongoTemplate mongoTemplate;
    private final BillRepository billRepository;

    public AnalyticsService(MongoTemplate mongoTemplate, BillRepository billRepository) {
        this.mongoTemplate = mongoTemplate;
        this.billRepository = billRepository;
    }

    public DashboardSummary summary(AuthUser user) {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        List<CategorySpend> thisMonth = categorySpend(user, monthStart);
        BigDecimal thisMonthSpend = thisMonth.stream().map(CategorySpend::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        String top = thisMonth.stream()
                .max(Comparator.comparing(CategorySpend::total))
                .map(CategorySpend::category)
                .orElse(null);

        List<MonthlySpend> recent = monthly(user, 6);
        BigDecimal totalSpend = recent.stream().map(MonthlySpend::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        // lifetime confirmed spend (not only 6 months)
        totalSpend = sumAllConfirmed(user);

        long confirmed = billRepository.countByUserIdAndStatus(user.id().toString(), BillStatus.CONFIRMED);
        long pending = billRepository.countByUserIdAndStatus(user.id().toString(), BillStatus.DRAFT);

        return new DashboardSummary(
                totalSpend,
                thisMonthSpend,
                confirmed,
                pending,
                top,
                thisMonth,
                recent,
                "INR"
        );
    }

    public List<MonthlySpend> monthly(AuthUser user, int months) {
        int window = months <= 0 ? 6 : months;
        LocalDate from = YearMonth.now().minusMonths(window - 1L).atDay(1);
        MatchOperation match = Aggregation.match(confirmed(user, from));
        GroupOperation group = Aggregation.group()
                .first("currency").as("currency")
                .sum("totalAmount").as("total")
                .count().as("billCount")
                .first("billDate").as("billDate");
        // Group by year-month via $dateToString equivalent: project first
        Aggregation aggregation = Aggregation.newAggregation(
                match,
                Aggregation.project("totalAmount", "billDate")
                        .andExpression("dateToString('%Y-%m', billDate)").as("month"),
                Aggregation.group("month")
                        .sum("totalAmount").as("total")
                        .count().as("billCount")
        );
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "bills", Document.class);
        Map<String, MonthlySpend> byMonth = new HashMap<>();
        for (Document doc : results.getMappedResults()) {
            String month = doc.getString("_id");
            byMonth.put(month, new MonthlySpend(month, toMoney(doc.get("total")), asLong(doc.get("billCount"))));
        }
        List<MonthlySpend> out = new ArrayList<>();
        YearMonth cursor = YearMonth.from(from);
        YearMonth end = YearMonth.now();
        while (!cursor.isAfter(end)) {
            String key = cursor.format(MONTH);
            out.add(byMonth.getOrDefault(key, new MonthlySpend(key, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0)));
            cursor = cursor.plusMonths(1);
        }
        return out;
    }

    public List<CategorySpend> categorySpend(AuthUser user, LocalDate from) {
        LocalDate start = from == null ? LocalDate.now().withDayOfMonth(1) : from;
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(confirmed(user, start)),
                Aggregation.group("category")
                        .sum("totalAmount").as("total")
                        .count().as("billCount")
        );
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "bills", Document.class);
        List<CategorySpend> raw = new ArrayList<>();
        BigDecimal grand = BigDecimal.ZERO;
        for (Document doc : results.getMappedResults()) {
            BigDecimal total = toMoney(doc.get("total"));
            grand = grand.add(total);
            Object id = doc.get("_id");
            raw.add(new CategorySpend(id == null ? "OTHER" : id.toString(), total, asLong(doc.get("billCount")), BigDecimal.ZERO));
        }
        List<CategorySpend> withShare = new ArrayList<>();
        for (CategorySpend row : raw) {
            BigDecimal share = grand.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    : row.total().divide(grand, 4, RoundingMode.HALF_UP);
            withShare.add(new CategorySpend(row.category(), row.total(), row.billCount(), share));
        }
        withShare.sort(Comparator.comparing(CategorySpend::total).reversed());
        return withShare;
    }

    private BigDecimal sumAllConfirmed(AuthUser user) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(confirmed(user, null)),
                Aggregation.group().sum("totalAmount").as("total")
        );
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "bills", Document.class);
        Document first = results.getUniqueMappedResult();
        if (first == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return toMoney(first.get("total"));
    }

    private static Criteria confirmed(AuthUser user, LocalDate from) {
        Criteria criteria = Criteria.where("userId").is(user.id().toString()).and("status").is(BillStatus.CONFIRMED);
        if (from != null) {
            criteria = criteria.and("billDate").gte(from);
        }
        return criteria;
    }

    static BigDecimal toMoney(Object value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Decimal128 d) {
            return d.bigDecimalValue().setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof BigDecimal b) {
            return b.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private static long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }
}
