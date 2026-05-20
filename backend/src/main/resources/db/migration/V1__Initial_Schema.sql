-- V1: Initial Schema for AI Workforce Intelligence Platform

CREATE TABLE permission (
    id UUID PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE role (
    id UUID PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE role_permission (
    role_id UUID NOT NULL REFERENCES role(id),
    permission_id UUID NOT NULL REFERENCES permission(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE account (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    role_id UUID REFERENCES role(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE organization (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    domain VARCHAR(255),
    logo_url VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE employee (
    id UUID PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    job_title VARCHAR(255),
    phone_number VARCHAR(255),
    account_id UUID NOT NULL REFERENCES account(id),
    team_id UUID, -- Foreign key added later to avoid circular dependency if needed, but we can declare it here as Team is created below. Wait! Team references Employee (manager_id). So we have a circular reference.
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE team (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(255),
    organization_id UUID REFERENCES organization(id),
    manager_id UUID REFERENCES employee(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Now add the foreign key for team_id on employee
ALTER TABLE employee ADD CONSTRAINT fk_employee_team FOREIGN KEY (team_id) REFERENCES team(id);

CREATE TABLE task (
    id UUID PRIMARY KEY,
    title VARCHAR(255),
    description TEXT,
    status VARCHAR(255),
    priority VARCHAR(255),
    due_date TIMESTAMP,
    estimated_hours INT,
    assignee_id UUID REFERENCES employee(id),
    reporter_id UUID REFERENCES employee(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE workload_event (
    id UUID PRIMARY KEY,
    event_type VARCHAR(255),
    event_details VARCHAR(255),
    impact_score INT,
    employee_id UUID REFERENCES employee(id),
    task_id UUID REFERENCES task(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE ai_insight (
    id UUID PRIMARY KEY,
    summary VARCHAR(255),
    full_analysis TEXT,
    severity VARCHAR(255),
    employee_id UUID REFERENCES employee(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE leave_request (
    id UUID PRIMARY KEY,
    reason VARCHAR(255),
    start_date DATE,
    end_date DATE,
    status VARCHAR(255),
    employee_id UUID REFERENCES employee(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE TABLE attendance (
    id UUID PRIMARY KEY,
    check_in_date DATE,
    check_in_time TIME,
    check_out_time TIME,
    status VARCHAR(255),
    employee_id UUID REFERENCES employee(id),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

-- Seed Initial Roles
INSERT INTO role (id, name, description, created_at) VALUES (gen_random_uuid(), 'ADMIN', 'Full system access', CURRENT_TIMESTAMP);
INSERT INTO role (id, name, description, created_at) VALUES (gen_random_uuid(), 'MANAGER', 'Team and workforce management', CURRENT_TIMESTAMP);
INSERT INTO role (id, name, description, created_at) VALUES (gen_random_uuid(), 'EMPLOYEE', 'Standard user access', CURRENT_TIMESTAMP);
