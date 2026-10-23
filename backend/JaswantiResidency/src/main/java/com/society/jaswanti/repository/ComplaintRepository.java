package com.society.jaswanti.repository;

import com.society.jaswanti.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByResidentId(Long residentId);
    List<Complaint> findByResidentIdOrderByCreatedDateDesc(Long residentId);
    List<Complaint> findAllByOrderByCreatedDateDesc();
    List<Complaint> findBySubmissionType(String submissionType);
    List<Complaint> findByResidentIdAndSubmissionType(Long residentId, String submissionType);
}
