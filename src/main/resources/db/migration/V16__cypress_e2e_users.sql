-- Dedicated users for Cypress E2E (announcement-create-api.cy.ts).
-- Passwords: teacher = professor123, admin = admin123 (BCrypt, cost 10; compatible with Spring Security).
-- Idempotent on email: safe if migration is restored/replayed on a copy where rows already exist.

INSERT INTO sisges.users (name, email, register, password, birth_date, gender, user_role)
VALUES (
    'Cypress E2E Teacher',
    'cypress-e2e-teacher@sisges.local',
    'CYPTCHR001',
    '$2b$10$5Sv4yZ/xv3WloaIQRo8NYefIrCJXemPriQvtC9fwpKgxbahPsO8lW',
    DATE '1990-01-15',
    'FEMALE',
    'TEACHER'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO sisges.users (name, email, register, password, birth_date, gender, user_role)
VALUES (
    'Cypress E2E Admin',
    'cypress-e2e-admin@sisges.local',
    'CYPADM001',
    '$2b$10$MW9PDAYDWAHg/2yb1KUE4uLidchUjU/YtDUk1NlQ4Nahoruyvm9lK',
    DATE '1985-06-20',
    'MALE',
    'ADMIN'
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO sisges.teacher (user_id)
SELECT u.id
FROM sisges.users u
WHERE u.email = 'cypress-e2e-teacher@sisges.local'
  AND NOT EXISTS (SELECT 1 FROM sisges.teacher t WHERE t.user_id = u.id);
