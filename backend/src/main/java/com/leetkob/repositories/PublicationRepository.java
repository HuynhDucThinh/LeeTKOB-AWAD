package com.leetkob.repositories;

import com.leetkob.entities.Publication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationRepository extends JpaRepository<Publication, Integer> {}
