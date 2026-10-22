-- Phase 2: Document legacy occupancy backfill limitation
-- In V5, existing legacy rooms.resident_id relationships were backfilled with a placeholder start_date ('2026-01-01').
-- Because the legacy schema did not capture historical move-in dates and room_residents carries no migration
-- provenance tracking, automated UPDATE queries cannot reliably distinguish original backfill rows from legitimate
-- relationships without risking data corruption (e.g. if rooms.resident_id or primary contact status was modified).
--
-- To prevent unintentional data loss or incorrect date erasure, automated bulk updates are intentionally avoided.
-- Administrators can inspect, verify, and update historical move-in/start dates manually using the Occupancy Registry UI.
SELECT 1;
