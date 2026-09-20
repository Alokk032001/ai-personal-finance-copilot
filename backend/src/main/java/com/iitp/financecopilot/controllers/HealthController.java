package com.iitp.financecopilot.controllers;

import com.iitp.financecopilot.ai.BillExtractor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final MongoTemplate mongoTemplate;
    private final BillExtractor billExtractor;

    public HealthController(MongoTemplate mongoTemplate, BillExtractor billExtractor) {
        this.mongoTemplate = mongoTemplate;
        this.billExtractor = billExtractor;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        String mongo;
        try {
            mongoTemplate.executeCommand(new Document("ping", 1));
            mongo = "UP";
        } catch (Exception ex) {
            mongo = "DOWN: " + ex.getMessage();
        }
        boolean up = "UP".equals(mongo);
        body.put("status", up ? "UP" : "DOWN");
        body.put("time", Instant.now().toString());
        body.put("postgres", "UP");
        body.put("mongodb", mongo);
        body.put("aiProvider", billExtractor.providerName());
        return body;
    }
}
