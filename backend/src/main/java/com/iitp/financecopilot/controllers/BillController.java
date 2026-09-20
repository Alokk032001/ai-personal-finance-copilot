package com.iitp.financecopilot.controllers;

import com.iitp.financecopilot.domain.BillStatus;
import com.iitp.financecopilot.dto.bill.BillResponse;
import com.iitp.financecopilot.dto.bill.BillUpdateRequest;
import com.iitp.financecopilot.security.AuthUser;
import com.iitp.financecopilot.services.BillService;
import com.iitp.financecopilot.services.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BillResponse upload(@AuthenticationPrincipal AuthUser user, @RequestParam("file") MultipartFile file) {
        return billService.upload(user, file);
    }

    @GetMapping
    public Page<BillResponse> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return billService.list(user, status, page, size);
    }

    @GetMapping("/{id}")
    public BillResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        return billService.get(user, id);
    }

    @PatchMapping("/{id}")
    public BillResponse patch(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable String id,
            @RequestBody(required = false) BillUpdateRequest request) {
        return billService.patch(user, id, request);
    }

    @PostMapping("/{id}/confirm")
    public BillResponse confirm(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable String id,
            @RequestBody(required = false) BillUpdateRequest request) {
        return billService.confirm(user, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        billService.delete(user, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> file(@AuthenticationPrincipal AuthUser user, @PathVariable String id) {
        FileStorageService.LoadedFile loaded = billService.file(user, id);
        String contentType = loaded.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : loaded.contentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + loaded.fileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(loaded.bytes());
    }
}
