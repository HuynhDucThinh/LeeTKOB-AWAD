DROP TABLE IF EXISTS publication_audit;
DROP TABLE IF EXISTS published_marks;
DROP TABLE IF EXISTS publications;
DROP TABLE IF EXISTS review_items;
DROP TABLE IF EXISTS counted_submissions;
DROP TABLE IF EXISTS ta_classes;
DROP TABLE IF EXISTS labs;

CREATE TABLE labs (
    id INT PRIMARY KEY CHECK (id > 0),
    class_id INT NOT NULL,
    deadline VARCHAR(64) NOT NULL
);

CREATE TABLE ta_classes (
    ta_id VARCHAR(64) NOT NULL,
    class_id INT NOT NULL,
    PRIMARY KEY (ta_id, class_id)
);

CREATE TABLE counted_submissions (
    id INT PRIMARY KEY,
    lab_id INT NOT NULL REFERENCES labs(id),
    student_id VARCHAR(64) NOT NULL,
    judge_status VARCHAR(32) NOT NULL CHECK (judge_status IN ('queued', 'running', 'AC', 'WA', 'TLE', 'MLE', 'RE', 'CE')),
    score_tenths INT NOT NULL CHECK (score_tenths BETWEEN 0 AND 100),
    UNIQUE (lab_id, student_id)
);

CREATE TABLE review_items (
    submission_id INT NOT NULL REFERENCES counted_submissions(id),
    constraint_id VARCHAR(64) NOT NULL,
    decision VARCHAR(32) NULL CHECK (decision IN ('pass', 'violation')),
    PRIMARY KEY (submission_id, constraint_id)
);

CREATE TABLE publications (
    lab_id INT PRIMARY KEY REFERENCES labs(id),
    receipt VARCHAR(4096) NOT NULL
);

CREATE TABLE published_marks (
    lab_id INT NOT NULL REFERENCES publications(lab_id),
    submission_id INT NOT NULL REFERENCES counted_submissions(id),
    score_tenths INT NOT NULL CHECK (score_tenths BETWEEN 0 AND 100),
    PRIMARY KEY (lab_id, submission_id)
);

CREATE TABLE publication_audit (
    lab_id INT PRIMARY KEY REFERENCES publications(lab_id),
    actor_id VARCHAR(64) NOT NULL,
    at VARCHAR(64) NOT NULL,
    event VARCHAR(64) NOT NULL CHECK (event = 'lab.published')
);
