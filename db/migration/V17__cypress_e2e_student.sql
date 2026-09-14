-- Cypress E2E student user with turma, discipline and teacher link.
-- Password: aluno123 (BCrypt cost 10).
-- Idempotent: safe to replay.

INSERT INTO sisges.users (name, email, register, password, birth_date, gender, user_role)
VALUES (
    'Cypress E2E Student',
    'cypress-e2e-student@sisges.local',
    'CYPSTU001',
    '$2b$10$IyC7PG/eDOFsblbCe8c/4OZOrmJ.0mL5pwwQB6nvZ3LeN5SAtBgmW',
    DATE '2008-03-10',
    'MALE',
    'STUDENT'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO sisges.student_responsible (name, phone, alternative_phone, email, alternative_email)
SELECT 'Responsável Cypress', '11988887777', '11988887778', 'resp-cypress@test.local', 'resp2-cypress@test.local'
WHERE NOT EXISTS (
    SELECT 1 FROM sisges.student_responsible WHERE email = 'resp-cypress@test.local'
);

INSERT INTO sisges.school_class (name, academic_year)
VALUES ('Turma Cypress E2E', '6º ano')
ON CONFLICT (name, academic_year) DO NOTHING;

INSERT INTO sisges.discipline (name, description)
VALUES ('Matemática Cypress', 'Disciplina para testes E2E')
ON CONFLICT (name) DO NOTHING;

INSERT INTO sisges.student (user_id, class_id)
SELECT u.id, sc.id
FROM sisges.users u
CROSS JOIN sisges.school_class sc
WHERE u.email = 'cypress-e2e-student@sisges.local'
  AND sc.name = 'Turma Cypress E2E'
  AND NOT EXISTS (SELECT 1 FROM sisges.student s WHERE s.user_id = u.id);

INSERT INTO sisges.student_responsible_link (student_id, responsible_id)
SELECT s.id, r.id
FROM sisges.student s
JOIN sisges.users u ON u.id = s.user_id
JOIN sisges.student_responsible r ON r.email = 'resp-cypress@test.local'
WHERE u.email = 'cypress-e2e-student@sisges.local'
ON CONFLICT (student_id, responsible_id) DO NOTHING;

INSERT INTO sisges.teacher_class (class_id, teacher_id)
SELECT sc.id, t.id
FROM sisges.school_class sc
JOIN sisges.teacher t ON t.user_id = (SELECT id FROM sisges.users WHERE email = 'cypress-e2e-teacher@sisges.local')
WHERE sc.name = 'Turma Cypress E2E'
ON CONFLICT (class_id, teacher_id) DO NOTHING;

INSERT INTO sisges.class_discipline (class_id, discipline_id)
SELECT sc.id, d.id
FROM sisges.school_class sc
JOIN sisges.discipline d ON d.name = 'Matemática Cypress'
WHERE sc.name = 'Turma Cypress E2E'
ON CONFLICT (class_id, discipline_id) DO NOTHING;
