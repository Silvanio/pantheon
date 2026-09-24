ALTER TABLE app_user ADD COLUMN super_admin BOOLEAN;

-- Seed the platform superadmin account. Password is "admin!12" (BCrypt-hashed below).
INSERT INTO app_user (id, email, display_name, password_hash, registration_status, super_admin, created_at, updated_at)
SELECT gen_random_uuid(), 'inextitsolutions@gmail.com', 'Inext', '$2a$10$htTeeWpvM55ghnh/Rft8h..WzZa6q82OVMcPfL.51/NiLAJvGxTj6', 'ACTIVE', true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email = 'inextitsolutions@gmail.com');
