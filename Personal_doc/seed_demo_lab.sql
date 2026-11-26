-- ============================================================================
-- Seed dữ liệu mẫu để nghiệm thu End-to-End (API -> DB -> Worker -> Docker Sandbox)
-- Lưu ý: Idempotent (chạy nhiều lần không bị lỗi trùng khóa).
-- ============================================================================

-- 1. Tạo lớp học mẫu (id = 1)
INSERT INTO classes (id, code, name)
VALUES (1, 'CSC10001', 'Nhập môn Lập trình C++')
ON CONFLICT (id) DO NOTHING;

-- 2. Tạo bài Lab mẫu (id = 9001, hạn nộp năm 2099)
INSERT INTO labs (id, class_id, deadline, title, statement_md, cost_cap_usd)
VALUES (
    9001,
    1,
    '2099-12-31T23:59:59Z',
    'Lab Demo: Tính tổng hai số nguyên A + B',
    'Viết chương trình C++ đọc hai số nguyên A và B từ stdin và in ra tổng A + B.',
    5.0000
)
ON CONFLICT (id) DO NOTHING;

-- 3. Nạp 3 Test Cases cho Lab 9001 (chỉ thêm nếu chưa có)
INSERT INTO lab_tests (lab_id, name, input_data, expected_output, time_limit_ms, memory_limit_mb, is_sample, order_index)
SELECT 9001, 'Test 1 - Số dương nhỏ', E'3 5\n', '8', 2000, 256, TRUE, 1
WHERE NOT EXISTS (SELECT 1 FROM lab_tests WHERE lab_id = 9001 AND order_index = 1);

INSERT INTO lab_tests (lab_id, name, input_data, expected_output, time_limit_ms, memory_limit_mb, is_sample, order_index)
SELECT 9001, 'Test 2 - Số âm và dương', E'-10 25\n', '15', 2000, 256, FALSE, 2
WHERE NOT EXISTS (SELECT 1 FROM lab_tests WHERE lab_id = 9001 AND order_index = 2);

INSERT INTO lab_tests (lab_id, name, input_data, expected_output, time_limit_ms, memory_limit_mb, is_sample, order_index)
SELECT 9001, 'Test 3 - Số nguyên 64-bit (long long)', E'1000000000 2000000000\n', '3000000000', 2000, 256, FALSE, 3
WHERE NOT EXISTS (SELECT 1 FROM lab_tests WHERE lab_id = 9001 AND order_index = 3);

-- 4. Nạp 1 ràng buộc mẫu cho Lab 9001
INSERT INTO lab_constraints (lab_id, constraint_id, constraint_type, description, deduction_tenths)
VALUES (9001, 'NO_FORBIDDEN_LIB', 'hard', 'Không sử dụng thư viện không chuẩn', 10)
ON CONFLICT (lab_id, constraint_id) DO NOTHING;

-- 5. Cập nhật sequence labs_id_seq để không bị đụng ID khi tạo lab mới từ API
SELECT setval('labs_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM labs), 9001));
