package com.leetkob.repositories;

import com.leetkob.entities.CountedSubmission;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountedSubmissionRepository extends JpaRepository<CountedSubmission, Integer> {

  long countByLabIdAndJudgeStatusIn(int labId, Collection<String> judgeStatuses);

  long countByLabId(int labId);

  List<CountedSubmission> findByLabId(int labId);
}
