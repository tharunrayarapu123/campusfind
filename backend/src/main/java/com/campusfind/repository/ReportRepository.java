package com.campusfind.repository;

import com.campusfind.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByItemId(Long itemId);
    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);
    boolean existsByItemIdAndReporterId(Long itemId, Long reporterId);
    long countByItemId(Long itemId);
}
