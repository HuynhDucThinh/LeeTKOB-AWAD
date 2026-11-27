package com.leetkob.repository;

import com.leetkob.entity.LlmCallLog;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LlmCallLogRepository extends JpaRepository<LlmCallLog, Long> {

  @Query("SELECT COALESCE(SUM(l.costUsd), 0) FROM LlmCallLog l WHERE l.lab.id = :labId")
  BigDecimal sumCostUsdByLabId(@Param("labId") int labId);
}
