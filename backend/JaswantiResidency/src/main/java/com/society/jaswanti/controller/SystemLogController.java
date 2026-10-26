package com.society.jaswanti.controller;

import com.society.jaswanti.audit.AuditTrail;
import com.society.jaswanti.audit.AuditedAction;
import com.society.jaswanti.dto.SystemLogDto;
import com.society.jaswanti.entity.ActivityLog;
import com.society.jaswanti.repository.ActivityLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Read-only access to the system audit log, for Master Admins only. There is intentionally no endpoint to edit
 * or delete entries.
 */
@RestController
@RequestMapping("/api/system-logs")
@PreAuthorize("hasRole('MASTER_ADMIN')")
public class SystemLogController {

    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_EXPORT_ROWS = 10_000;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("timestamp"), Sort.Order.desc("id"));

    private final ActivityLogRepository activityLogRepository;

    public SystemLogController(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<ActivityLog> result = activityLogRepository.findAll(
                filters(q, module, action, outcome, actorRole, from, to),
                PageRequest.of(Math.max(page, 0), pageSize, NEWEST_FIRST));
        return ResponseEntity.ok(Map.of(
                "content", result.getContent().stream().map(SystemLogDto::from).toList(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages()));
    }

    @GetMapping("/filters")
    public ResponseEntity<Map<String, List<String>>> filterOptions() {
        return ResponseEntity.ok(Map.of(
                "modules", activityLogRepository.findDistinctModules(),
                "actions", activityLogRepository.findDistinctActions(),
                "outcomes", List.of(ActivityLog.OUTCOME_SUCCESS, ActivityLog.OUTCOME_FAILED, ActivityLog.OUTCOME_DENIED)));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @AuditedAction(action = "EXPORT_SYSTEM_LOGS", module = "SYSTEM", summary = "Exported system logs to CSV")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<ActivityLog> rows = activityLogRepository.findAll(
                filters(q, module, action, outcome, actorRole, from, to),
                PageRequest.of(0, MAX_EXPORT_ROWS, NEWEST_FIRST)).getContent();

        StringBuilder csv = new StringBuilder("Timestamp,Actor,Actor Email,Role,Module,Action,Outcome,Target,Description,Method,Path,IP\n");
        for (ActivityLog l : rows) {
            csv.append(String.join(",", cell(l.getTimestamp() != null ? l.getTimestamp().toString() : ""),
                    cell(l.getActorName()), cell(l.getActorEmail()), cell(l.getActorRole()), cell(l.getModule()),
                    cell(l.getAction()), cell(l.getOutcome()), cell(l.getTargetLabel()), cell(l.getDescription()),
                    cell(l.getHttpMethod()), cell(l.getRequestPath()), cell(l.getIpAddress()))).append('\n');
        }
        AuditTrail.describe("Exported " + rows.size() + " system log entries to CSV"
                + (rows.size() == MAX_EXPORT_ROWS ? " (row limit reached)" : ""));

        String filename = "system-logs-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    static Specification<ActivityLog> filters(String q, String module, String action, String outcome, String actorRole,
                                              LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (notBlank(module)) predicates.add(cb.equal(root.get("module"), module));
            if (notBlank(action)) predicates.add(cb.equal(root.get("action"), action));
            if (notBlank(outcome)) predicates.add(cb.equal(root.get("outcome"), outcome));
            if (notBlank(actorRole)) predicates.add(cb.equal(root.get("actorRole"), actorRole));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from.atStartOfDay()));
            if (to != null) predicates.add(cb.lessThan(root.get("timestamp"), to.plusDays(1).atStartOfDay()));
            if (notBlank(q)) {
                String like = "%" + q.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("description")), like, '\\'),
                        cb.like(cb.lower(root.get("actorName")), like, '\\'),
                        cb.like(cb.lower(root.get("actorEmail")), like, '\\'),
                        cb.like(cb.lower(root.get("targetLabel")), like, '\\'),
                        cb.like(cb.lower(root.get("action")), like, '\\')));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** CSV-escapes a value and neutralises spreadsheet formula injection (=, +, -, @). */
    static String cell(String value) {
        if (value == null) {
            return "";
        }
        String v = value;
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
