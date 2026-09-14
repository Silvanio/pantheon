## MODIFIED Requirements

### Requirement: User profile registration
`pantheon-service` SHALL allow an authenticated user to record their CNPJ/CPF, legal name (Nome/Razão Social), address, and postal code (CEP) as their own profile data, independent of any specific project. Submitting this data again SHALL update the user's existing profile rather than creating a duplicate. `pantheon-service` SHALL also allow an authenticated user to retrieve their own profile data.

#### Scenario: Profile created on first submission
- **WHEN** an authenticated user with no existing profile submits CNPJ/CPF, legal name, address, and CEP
- **THEN** `pantheon-service` persists a new profile record associated with that user

#### Scenario: Profile updated on subsequent submission
- **WHEN** an authenticated user who already has a profile submits CNPJ/CPF, legal name, address, and CEP again
- **THEN** `pantheon-service` updates the existing profile record for that user rather than creating a second one

#### Scenario: Profile retrieved
- **WHEN** an authenticated user with an existing profile requests their own profile data
- **THEN** `pantheon-service` returns their CNPJ/CPF, legal name, address, and CEP

#### Scenario: No profile yet
- **WHEN** an authenticated user with no existing profile requests their own profile data
- **THEN** `pantheon-service` indicates no profile exists rather than an error
