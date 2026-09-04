CREATE TABLE user_profile (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL UNIQUE REFERENCES app_user (id),
    cnpj_cpf       VARCHAR(20) NOT NULL,
    legal_name     VARCHAR(255) NOT NULL,
    address        VARCHAR(255) NOT NULL,
    postal_code    VARCHAR(10) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
