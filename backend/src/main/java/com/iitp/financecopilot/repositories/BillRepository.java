package com.iitp.financecopilot.repositories;

import com.iitp.financecopilot.domain.Bill;
import com.iitp.financecopilot.domain.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BillRepository extends MongoRepository<Bill, String> {

    Page<Bill> findByUserId(String userId, Pageable pageable);

    Page<Bill> findByUserIdAndStatus(String userId, BillStatus status, Pageable pageable);

    Optional<Bill> findByIdAndUserId(String id, String userId);

    long countByUserIdAndStatus(String userId, BillStatus status);
}
