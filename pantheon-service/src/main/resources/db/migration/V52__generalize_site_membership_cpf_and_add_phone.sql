ALTER TABLE site_membership RENAME COLUMN client_cpf TO cpf;
ALTER TABLE site_membership ADD COLUMN phone VARCHAR(20);
