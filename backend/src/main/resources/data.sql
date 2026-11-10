INSERT INTO labs (id, class_id, deadline) VALUES (1, 10, '2026-11-05T08:00:00.000Z');
INSERT INTO labs (id, class_id, deadline) VALUES (2, 20, '2026-11-05T08:00:00.000Z');

INSERT INTO ta_classes (ta_id, class_id) VALUES ('ta-own', 10);
INSERT INTO ta_classes (ta_id, class_id) VALUES ('ta-colleague', 10);
INSERT INTO ta_classes (ta_id, class_id) VALUES ('ta-foreign', 20);
INSERT INTO ta_classes (ta_id, class_id) VALUES ('ta-demo', 10);

INSERT INTO counted_submissions (id, lab_id, student_id, judge_status, score_tenths)
VALUES (11, 1, 'student-1', 'AC', 70);
INSERT INTO counted_submissions (id, lab_id, student_id, judge_status, score_tenths)
VALUES (12, 1, 'student-2', 'WA', 0);

INSERT INTO review_items (submission_id, constraint_id, decision)
VALUES (11, 'C1', 'violation');
INSERT INTO review_items (submission_id, constraint_id, decision)
VALUES (11, 'C2', 'pass');
