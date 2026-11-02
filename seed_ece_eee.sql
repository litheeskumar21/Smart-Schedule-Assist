-- Insert ECE Subjects (Dept 2, Year 2)
INSERT INTO subjects (id, code, name, periods_per_week, type, academic_year_id, department_id) VALUES
(9, 'EC201', 'Signals and Systems', 4, 'THEORY', 2, 2),
(10, 'EC202', 'Analog Circuits', 4, 'THEORY', 2, 2),
(11, 'EC203', 'Digital Electronics', 3, 'THEORY', 2, 2),
(12, 'EC204', 'Electromagnetic Fields', 3, 'THEORY', 2, 2),
(13, 'EC205', 'Microprocessors & Microcontrollers', 4, 'THEORY', 2, 2),
(14, 'EC206', 'Analog Circuits Lab', 2, 'LAB', 2, 2),
(15, 'EC207', 'Digital Electronics Lab', 2, 'LAB', 2, 2),
(16, 'EC208', 'Microprocessors Lab', 2, 'LAB', 2, 2)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Insert EEE Subjects (Dept 3, Year 2)
INSERT INTO subjects (id, code, name, periods_per_week, type, academic_year_id, department_id) VALUES
(17, 'EE201', 'Electric Circuit Theory', 4, 'THEORY', 2, 3),
(18, 'EE202', 'Electrical Machines - I', 4, 'THEORY', 2, 3),
(19, 'EE203', 'Control Systems', 3, 'THEORY', 2, 3),
(20, 'EE204', 'Power Electronics', 3, 'THEORY', 2, 3),
(21, 'EE205', 'Measurements & Instrumentation', 4, 'THEORY', 2, 3),
(22, 'EE206', 'Electric Circuits Lab', 2, 'LAB', 2, 3),
(23, 'EE207', 'Electrical Machines Lab', 2, 'LAB', 2, 3),
(24, 'EE208', 'Power Electronics Lab', 2, 'LAB', 2, 3)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Subject Assignments for ECE-A (section 3)
INSERT INTO subject_assignments (section_id, subject_id, teacher_id) VALUES
(3, 9, 4),   -- EC201 -> T004 (Prof. Reddy)
(3, 10, 5),  -- EC202 -> T005 (Dr. Singh)
(3, 11, 13), -- EC203 -> T013 (Dr. Rao)
(3, 12, 3),  -- EC204 -> T003 (Dr. Patel)
(3, 13, 14), -- EC205 -> T014 (Prof. Pillai)
(3, 14, 5),  -- EC206 -> T005 (Dr. Singh)
(3, 15, 13), -- EC207 -> T013 (Dr. Rao)
(3, 16, 14)  -- EC208 -> T014 (Prof. Pillai)
ON DUPLICATE KEY UPDATE teacher_id=VALUES(teacher_id);

-- Subject Assignments for ECE-B (section 4)
INSERT INTO subject_assignments (section_id, subject_id, teacher_id) VALUES
(4, 9, 5),   -- EC201 -> T005 (Dr. Singh)
(4, 10, 4),  -- EC202 -> T004 (Prof. Reddy)
(4, 11, 3),  -- EC203 -> T003 (Dr. Patel)
(4, 12, 13), -- EC204 -> T013 (Dr. Rao)
(4, 13, 4),  -- EC205 -> T004 (Prof. Reddy)
(4, 14, 4),  -- EC206 -> T004 (Prof. Reddy)
(4, 15, 3),  -- EC207 -> T003 (Dr. Patel)
(4, 16, 5)   -- EC208 -> T005 (Dr. Singh)
ON DUPLICATE KEY UPDATE teacher_id=VALUES(teacher_id);

-- Subject Assignments for EEE-A (section 5)
INSERT INTO subject_assignments (section_id, subject_id, teacher_id) VALUES
(5, 17, 6),  -- EE201 -> T006 (Prof. Gupta)
(5, 18, 6),  -- EE202 -> T006 (Prof. Gupta)
(5, 19, 7),  -- EE203 -> T007 (Dr. Verma)
(5, 20, 7),  -- EE204 -> T007 (Dr. Verma)
(5, 21, 14), -- EE205 -> T014 (Prof. Pillai)
(5, 22, 7),  -- EE206 -> T007 (Dr. Verma)
(5, 23, 6),  -- EE207 -> T006 (Prof. Gupta)
(5, 24, 14)  -- EE208 -> T014 (Prof. Pillai)
ON DUPLICATE KEY UPDATE teacher_id=VALUES(teacher_id);
