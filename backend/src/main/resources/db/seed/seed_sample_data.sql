-- ============================================================
-- seed_sample_data.sql — Dữ liệu mẫu idempotent cho LeeTKOB (PostgreSQL)
-- Chạy bằng lệnh: npm run db:seed (từ thư mục gốc)
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Lớp học (Classes)
INSERT INTO classes (id, code, name) VALUES
    (10, 'CSC13114-A', 'Intro to C++ — Class 10'),
    (20, 'CSC13114-B', 'Intro to C++ — Class 20')
ON CONFLICT (id) DO NOTHING;

-- 2. 5 tài khoản test mẫu (mã hóa BCrypt trực tiếp bằng pgcrypto)
--    admin@leetkob.edu    / Admin@123
--    ta1@leetkob.edu      / Ta1@1234
--    ta2@leetkob.edu      / Ta2@1234
--    student1@leetkob.edu / Student1@123
--    student2@leetkob.edu / Student2@123
INSERT INTO users (email, password_hash, full_name, role) VALUES
    ('admin@leetkob.edu',    crypt('Admin@123',    gen_salt('bf', 10)), 'System Admin', 'admin'),
    ('ta1@leetkob.edu',      crypt('Ta1@1234',     gen_salt('bf', 10)), 'TA Class 10',  'ta'),
    ('ta2@leetkob.edu',      crypt('Ta2@1234',     gen_salt('bf', 10)), 'TA Class 20',  'ta'),
    ('student1@leetkob.edu', crypt('Student1@123', gen_salt('bf', 10)), 'Student 1',    'student'),
    ('student2@leetkob.edu', crypt('Student2@123', gen_salt('bf', 10)), 'Student 2',    'student')
ON CONFLICT (email) DO NOTHING;

-- 3. Phân công thành viên vào lớp (class_members)
INSERT INTO class_members (class_id, user_id, member_role)
SELECT 10, id, 'ta' FROM users WHERE email = 'ta1@leetkob.edu'
ON CONFLICT (class_id, user_id) DO NOTHING;

INSERT INTO class_members (class_id, user_id, member_role)
SELECT 20, id, 'ta' FROM users WHERE email = 'ta2@leetkob.edu'
ON CONFLICT (class_id, user_id) DO NOTHING;

INSERT INTO class_members (class_id, user_id, member_role)
SELECT 10, id, 'student' FROM users WHERE email = 'student1@leetkob.edu'
ON CONFLICT (class_id, user_id) DO NOTHING;

INSERT INTO class_members (class_id, user_id, member_role)
SELECT 10, id, 'student' FROM users WHERE email = 'student2@leetkob.edu'
ON CONFLICT (class_id, user_id) DO NOTHING;

-- 4. Dữ liệu mẫu cho bảng Lab Publish (PA#2)
INSERT INTO labs (id, class_id, deadline) VALUES
    (1, 10, '2026-11-01T00:00:00Z'),
    (2, 20, '2026-11-01T00:00:00Z')
ON CONFLICT (id) DO NOTHING;

INSERT INTO ta_classes (ta_id, class_id) VALUES
    ('ta-demo', 10),
    ('ta1@leetkob.edu', 10),
    ('ta2@leetkob.edu', 20)
ON CONFLICT (ta_id, class_id) DO NOTHING;

INSERT INTO counted_submissions (id, lab_id, student_id, judge_status, score_tenths) VALUES
    (101, 1, 'student1@leetkob.edu', 'AC', 100),
    (102, 1, 'student2@leetkob.edu', 'AC', 85)
ON CONFLICT (id) DO NOTHING;

INSERT INTO review_items (submission_id, constraint_id, decision) VALUES
    (101, 'C1', 'pass'),
    (102, 'C1', 'violation')
ON CONFLICT (submission_id, constraint_id) DO NOTHING;
