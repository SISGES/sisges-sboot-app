-- Test fixtures must never survive in an application database.
-- This forward migration preserves the checksum of the already-applied V17 migration.

DELETE FROM sisges.users
WHERE email = 'cypress-e2e-student@sisges.local';

DELETE FROM sisges.student_responsible
WHERE email = 'resp-cypress@test.local'
  AND NOT EXISTS (
      SELECT 1
      FROM sisges.student_responsible_link link
      WHERE link.responsible_id = sisges.student_responsible.id
  );

DELETE FROM sisges.school_class
WHERE name = 'Turma Cypress E2E'
  AND academic_year = '6º ano'
  AND NOT EXISTS (
      SELECT 1
      FROM sisges.student student
      WHERE student.class_id = sisges.school_class.id
  );

DELETE FROM sisges.discipline
WHERE name = 'Matemática Cypress'
  AND NOT EXISTS (
      SELECT 1
      FROM sisges.class_discipline class_discipline
      WHERE class_discipline.discipline_id = sisges.discipline.id
  );
