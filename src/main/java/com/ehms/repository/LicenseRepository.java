package com.ehms.repository;

import com.ehms.model.License;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Integer> {
    Optional<License> findTopByOrderByIdDesc();
    List<License> findAllByOrderByIdDesc();
}
