CREATE TABLE users (
    id               BINARY(16)    NOT NULL PRIMARY KEY,
    provider         VARCHAR(20)   NOT NULL,
    provider_user_id VARCHAR(255)  NOT NULL,
    email            VARCHAR(255)  NULL,
    display_name     VARCHAR(255)  NOT NULL,
    avatar_url       VARCHAR(2048) NULL,
    timezone         VARCHAR(64)   NOT NULL DEFAULT 'UTC',
    created_at       DATETIME(6)   NOT NULL,
    last_login_at    DATETIME(6)   NOT NULL,
    CONSTRAINT uk_users_identity UNIQUE (provider, provider_user_id),
    CONSTRAINT ck_users_provider CHECK (provider IN ('GOOGLE', 'GITHUB'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE projects (
    id         BINARY(16)   NOT NULL PRIMARY KEY,
    user_id    BINARY(16)   NOT NULL,
    name       VARCHAR(150) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    CONSTRAINT fk_projects_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_projects_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tasks (
    id          BINARY(16)   NOT NULL PRIMARY KEY,
    project_id  BINARY(16)   NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT         NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'TODO',
    priority    VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    due_date    DATE         NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE RESTRICT,
    CONSTRAINT ck_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT ck_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    INDEX idx_tasks_project_status_due (project_id, status, due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
