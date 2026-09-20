package com.iitp.financecopilot.services;

import com.iitp.financecopilot.ai.BillExtractor;
import com.iitp.financecopilot.ai.BillNormalizer;
import com.iitp.financecopilot.ai.ExtractionResult;
import com.iitp.financecopilot.common.ApiException;
import com.iitp.financecopilot.domain.Bill;
import com.iitp.financecopilot.domain.BillItem;
import com.iitp.financecopilot.domain.BillStatus;
import com.iitp.financecopilot.domain.ExpenseCategory;
import com.iitp.financecopilot.domain.ExtractionMeta;
import com.iitp.financecopilot.domain.SourceFile;
import com.iitp.financecopilot.dto.bill.BillItemPayload;
import com.iitp.financecopilot.dto.bill.BillResponse;
import com.iitp.financecopilot.dto.bill.BillUpdateRequest;
import com.iitp.financecopilot.repositories.BillRepository;
import com.iitp.financecopilot.security.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class BillService {

    private static final Set<String> ALLOWED_MIME = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "application/pdf"
    );

    private final BillExtractor billExtractor;
    private final BillNormalizer billNormalizer;
    private final FileStorageService fileStorageService;
    private final BillRepository billRepository;

    public BillService(
            BillExtractor billExtractor,
            BillNormalizer billNormalizer,
            FileStorageService fileStorageService,
            BillRepository billRepository) {
        this.billExtractor = billExtractor;
        this.billNormalizer = billNormalizer;
        this.fileStorageService = fileStorageService;
        this.billRepository = billRepository;
    }

    public BillResponse upload(AuthUser user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported file type. Use JPEG, PNG, WEBP, or PDF.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read uploaded file");
        }
        if (bytes.length > 10 * 1024 * 1024) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File must be 10 MB or smaller");
        }

        ExtractionResult extraction = billExtractor.extract(bytes, contentType);

        Bill bill = new Bill();
        bill.setUserId(user.id().toString());
        ExtractionMeta meta = new ExtractionMeta();
        meta.setProvider(extraction.provider());
        meta.setModel(extraction.model());
        meta.setLatencyMs(extraction.latencyMs());
        meta.setRawResponse(extraction.rawResponse());
        bill.setExtraction(meta);

        billNormalizer.normalizeInto(bill, extraction.extracted());

        // GridFS only after extract+normalize succeeds — no orphan blobs.
        String storageId = fileStorageService.store(
                file.getOriginalFilename() == null ? "bill" : file.getOriginalFilename(),
                contentType,
                bytes);
        SourceFile source = new SourceFile();
        source.setFileName(file.getOriginalFilename());
        source.setContentType(contentType);
        source.setSizeBytes(bytes.length);
        source.setStorageId(storageId);
        bill.setSourceFile(source);

        Instant now = Instant.now();
        bill.setCreatedAt(now);
        bill.setUpdatedAt(now);
        bill.setStatus(BillStatus.DRAFT);
        return toResponse(billRepository.save(bill));
    }

    public Page<BillResponse> list(AuthUser user, BillStatus status, int page, int size) {
        int capped = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), capped, Sort.by(Sort.Direction.DESC, "billDate"));
        Page<Bill> bills = status == null
                ? billRepository.findByUserId(user.id().toString(), pageable)
                : billRepository.findByUserIdAndStatus(user.id().toString(), status, pageable);
        return bills.map(this::toResponse);
    }

    public BillResponse get(AuthUser user, String id) {
        return toResponse(owned(user, id));
    }

    public BillResponse patch(AuthUser user, String id, BillUpdateRequest request) {
        Bill bill = owned(user, id);
        if (bill.getStatus() == BillStatus.CONFIRMED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Confirmed bills cannot be edited");
        }
        applyUpdates(bill, request);
        bill.setUpdatedAt(Instant.now());
        return toResponse(billRepository.save(bill));
    }

    public BillResponse confirm(AuthUser user, String id, BillUpdateRequest request) {
        Bill bill = owned(user, id);
        if (bill.getStatus() == BillStatus.CONFIRMED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bill is already confirmed");
        }
        if (request != null) {
            applyUpdates(bill, request);
        }
        if (bill.getTotalAmount() == null || bill.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a total amount greater than zero before saving");
        }
        bill.setStatus(BillStatus.CONFIRMED);
        Instant now = Instant.now();
        bill.setConfirmedAt(now);
        bill.setUpdatedAt(now);
        return toResponse(billRepository.save(bill));
    }

    public void delete(AuthUser user, String id) {
        Bill bill = owned(user, id);
        if (bill.getSourceFile() != null) {
            fileStorageService.delete(bill.getSourceFile().getStorageId());
        }
        billRepository.delete(bill);
    }

    public FileStorageService.LoadedFile file(AuthUser user, String id) {
        Bill bill = owned(user, id);
        if (bill.getSourceFile() == null || bill.getSourceFile().getStorageId() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Bill not found");
        }
        return fileStorageService.load(bill.getSourceFile().getStorageId());
    }

    private Bill owned(AuthUser user, String id) {
        return billRepository.findByIdAndUserId(id, user.id().toString())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Bill not found"));
    }

    private void applyUpdates(Bill bill, BillUpdateRequest request) {
        if (request == null) {
            return;
        }
        boolean changed = false;
        if (request.merchantName() != null && !Objects.equals(request.merchantName(), bill.getMerchantName())) {
            bill.setMerchantName(request.merchantName());
            changed = true;
        }
        if (request.invoiceNumber() != null && !Objects.equals(request.invoiceNumber(), bill.getInvoiceNumber())) {
            bill.setInvoiceNumber(request.invoiceNumber());
            changed = true;
        }
        if (request.billDate() != null && !Objects.equals(request.billDate(), bill.getBillDate())) {
            bill.setBillDate(request.billDate());
            changed = true;
        }
        if (request.totalAmount() != null && (bill.getTotalAmount() == null
                || request.totalAmount().compareTo(bill.getTotalAmount()) != 0)) {
            bill.setTotalAmount(request.totalAmount());
            changed = true;
        }
        if (request.currency() != null) {
            if (request.currency().length() != 3) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Currency must be a 3-letter code");
            }
            String currency = request.currency().toUpperCase(Locale.ROOT);
            if (!currency.equals(bill.getCurrency())) {
                bill.setCurrency(currency);
                changed = true;
            }
        }
        if (request.category() != null && request.category() != bill.getCategory()) {
            bill.setCategory(request.category());
            bill.setCategoryConfidence(1.0);
            changed = true;
        }
        if (request.paymentMethod() != null) {
            String pay = request.paymentMethod().toUpperCase(Locale.ROOT);
            if (!pay.equals(bill.getPaymentMethod())) {
                bill.setPaymentMethod(pay);
                changed = true;
            }
        }
        if (request.items() != null) {
            bill.setItems(fromPayload(request.items()));
            changed = true;
        }
        if (changed) {
            bill.setUserEdited(true);
        }
    }

    private static List<BillItem> fromPayload(List<BillItemPayload> payloads) {
        List<BillItem> items = new ArrayList<>();
        for (BillItemPayload payload : payloads) {
            BillItem item = new BillItem();
            item.setName(payload.name());
            item.setQuantity(payload.quantity());
            item.setAmount(payload.amount());
            item.setCategory(payload.category() == null ? ExpenseCategory.OTHER : payload.category());
            items.add(item);
        }
        return items;
    }

    public BillResponse toResponse(Bill bill) {
        ExtractionMeta meta = bill.getExtraction();
        List<BillItemPayload> items = new ArrayList<>();
        if (bill.getItems() != null) {
            for (BillItem item : bill.getItems()) {
                items.add(new BillItemPayload(item.getName(), item.getQuantity(), item.getAmount(), item.getCategory()));
            }
        }
        return new BillResponse(
                bill.getId(),
                bill.getStatus(),
                bill.getMerchantName(),
                bill.getInvoiceNumber(),
                bill.getBillDate(),
                bill.getTotalAmount(),
                bill.getCurrency(),
                bill.getCategory(),
                bill.getCategoryConfidence(),
                bill.getPaymentMethod(),
                items,
                bill.getSourceFile() == null ? null : bill.getSourceFile().getFileName(),
                meta == null ? List.of() : meta.getWarnings(),
                meta == null ? null : meta.getProvider(),
                meta == null ? null : meta.getModel(),
                meta == null ? null : meta.getLatencyMs(),
                bill.isUserEdited(),
                bill.getCreatedAt(),
                bill.getConfirmedAt()
        );
    }
}
