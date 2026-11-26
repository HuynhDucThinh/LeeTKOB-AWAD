package com.leetkob.repositories;

import com.leetkob.entities.PublishedMark;
import com.leetkob.entities.ids.PublishedMarkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublishedMarkRepository extends JpaRepository<PublishedMark, PublishedMarkId> {}
