package com.example.memberservice.repository;

import com.example.memberservice.domain.QueryHistory;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface QueryHistoryRepository extends ReactiveCrudRepository<QueryHistory, Long> {
}
