package com.iitp.financecopilot;

import com.iitp.financecopilot.ai.BillExtractor;
import com.iitp.financecopilot.repositories.BillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextTest {

    @MockitoBean
    private BillRepository billRepository;

    @MockitoBean
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private BillExtractor billExtractor;

    @Test
    void wiredExtractorIsStub() {
        assertEquals("stub", billExtractor.providerName());
    }
}
