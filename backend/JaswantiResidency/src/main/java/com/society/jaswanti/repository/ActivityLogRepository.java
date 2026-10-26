package com.society.jaswanti.repository;

import com.society.jaswanti.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long>, JpaSpecificationExecutor<ActivityLog> {

    @Query("SELECT DISTINCT l.module FROM ActivityLog l ORDER BY l.module")
    List<String> findDistinctModules();

    @Query("SELECT DISTINCT l.action FROM ActivityLog l ORDER BY l.action")
    List<String> findDistinctActions();
}
