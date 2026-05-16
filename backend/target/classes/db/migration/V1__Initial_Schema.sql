-- V1: Initial Schema for AI Workforce Intelligence Platform

CREATE TABLE permission (
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    module VARCHAR(50),
    action VARCHAR(20),
    description VARCHAR(255)
);

CREATE TABLE role (
    id UUID PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    is_system BOOLEAN DEFAULT TRUE
);

CREATE TABLE role_permission (
    role_id UUID NOT NULL REFERENCES role(id),
    permission_id UUID NOT NULL REFERENCES permission(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE organization (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    domain VARCHAR(100),
    industry VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE TABLE account (
    id UUID PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_id UUID NOT NULL REFERENCES role(id),
    is_active BOOLEAN DEFAULT TRUE,
    is_locked BOOLEAN DEFAULT FALSE,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP,
    refresh_token VARCHAR(500),
    refresh_token_expiry TIMESTAMP,
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(45),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE TABLE employee (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL UNIQUE REFERENCES account(id),
    full_name VARCHAR(200) NOT NULL,
    position VARCHAR(100),
    department VARCHAR(100),
    organization_id UUID REFERENCES organization(id),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE TABLE task (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(50) NOT NULL,
    due_date TIMESTAMP,
    assignee_id UUID REFERENCES employee(id),
    creator_id UUID REFERENCES employee(id),
    estimated_hours INT,
    actual_hours INT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE TABLE workload_event (
    id UUID PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    employee_id UUID REFERENCES employee(id),
    entity_type VARCHAR(50),
    entity_id VARCHAR(50),
    metadata TEXT,
    timestamp TIMESTAMP NOT NULL
);

-- Seed Initial Roles
INSERT INTO role (id, name, description) VALUES (gen_random_uuid(), 'ADMIN', 'Full system access');
INSERT INTO role (id, name, description) VALUES (gen_random_uuid(), 'MANAGER', 'Team and workforce management');
INSERT INTO role (id, name, description) VALUES (gen_random_uuid(), 'EMPLOYEE', 'Standard user access');
