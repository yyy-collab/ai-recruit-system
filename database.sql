CREATE DATABASE IF NOT EXISTS resume_filter
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;

USE resume_filter;

CREATE TABLE IF NOT EXISTS recruiter_user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    company_name VARCHAR(100),
    email VARCHAR(100) NOT NULL UNIQUE,
    avatar_url VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS job_seeker_user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS job (
    id INT PRIMARY KEY AUTO_INCREMENT,
    job_name VARCHAR(100) NOT NULL,
    job_desc TEXT,
    requirement TEXT,
    keywords TEXT NOT NULL,
    salary VARCHAR(50),
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS resume (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    resume_file_name VARCHAR(100),
    resume_file_url VARCHAR(255) NOT NULL,
    is_parsed TINYINT(1) DEFAULT 0,
    total_score INT,
    keyword_coverage INT,
    semantic_complete INT,
    competitiveness INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_resume_user FOREIGN KEY (user_id) REFERENCES job_seeker_user(id)
);

CREATE TABLE IF NOT EXISTS resume_ai_analysis (
    id INT PRIMARY KEY AUTO_INCREMENT,
    resume_id INT NOT NULL,
    job_post_id INT NOT NULL,
    match_score INT,
    match_level VARCHAR(20),
    ai_comment TEXT,
    core_advantages TEXT,
    potential_risks TEXT,
    skill_tags VARCHAR(255),
    analysis_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_analysis_resume FOREIGN KEY (resume_id) REFERENCES resume(id),
    CONSTRAINT fk_analysis_job FOREIGN KEY (job_post_id) REFERENCES job(id)
);

CREATE TABLE IF NOT EXISTS delivery (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    job_id INT NOT NULL,
    resume_id INT NOT NULL,
    match_score DECIMAL(5,2),
    status INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_user FOREIGN KEY (user_id) REFERENCES job_seeker_user(id),
    CONSTRAINT fk_delivery_job FOREIGN KEY (job_id) REFERENCES job(id),
    CONSTRAINT fk_delivery_resume FOREIGN KEY (resume_id) REFERENCES resume(id)
);
