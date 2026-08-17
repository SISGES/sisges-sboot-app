-- Ensure a default academic cycle row exists for first-time environments.

INSERT INTO sisges.academic_cycle (status, current_trimester, grading_locked)
SELECT 'NOT_STARTED', 1, FALSE
WHERE NOT EXISTS (SELECT 1 FROM sisges.academic_cycle);
