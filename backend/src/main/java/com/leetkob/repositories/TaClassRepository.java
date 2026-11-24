package com.leetkob.repositories;

import com.leetkob.entities.TaClass;
import com.leetkob.entities.ids.TaClassId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaClassRepository extends JpaRepository<TaClass, TaClassId> {}
