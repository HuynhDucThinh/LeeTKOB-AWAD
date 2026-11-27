package com.leetkob.repository;

import com.leetkob.entity.PublishedMark;
import com.leetkob.entity.id.PublishedMarkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublishedMarkRepository extends JpaRepository<PublishedMark, PublishedMarkId> {}
