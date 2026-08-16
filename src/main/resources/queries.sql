-- ============================================================
-- ClinicSystem - sample data for local testing
-- Run against the 'clinic' database:
--   docker exec -i clinic-mysql mysql -uroot -p123123 clinic < queries.sql
-- or paste the file into MySQL Workbench / DBeaver / IntelliJ.
--
-- Primary keys are auto-generated (IDENTITY); parent/child rows of the
-- JOINED inheritance are linked via LAST_INSERT_ID().
--
-- Passwords (BCrypt hashes are already generated, plain text shown for you):
--   doctor1@clinic.com    -> Doctor@123
--   doctor2@clinic.com    -> Doctor@123
--   patient1@clinic.com   -> Patient@123
--   patient2@clinic.com   -> Patient@123
--   admin@clinic.com      -> Admin@12345  (created automatically on app startup)
--
-- NOTE: inserts are safe to re-run only once (unique email / unique schedule).
-- ============================================================

USE clinic;

-- ---------- Users ----------
INSERT INTO users (name, email, phone, password, role) VALUES
('Dr. Sarah Ahmed', 'doctor1@clinic.com', '01000000001', '$2a$10$zSf48km8ca2850WDco9AruTricNtVHCPcmeJLT4xcq923Hme1MJUK', 'DOCTOR');
SET @doc1 = LAST_INSERT_ID();

INSERT INTO users (name, email, phone, password, role) VALUES
('Dr. Omar Hassan', 'doctor2@clinic.com', '01000000002', '$2a$10$zSf48km8ca2850WDco9AruTricNtVHCPcmeJLT4xcq923Hme1MJUK', 'DOCTOR');
SET @doc2 = LAST_INSERT_ID();

INSERT INTO users (name, email, phone, password, role) VALUES
('Mona El-Sayed', 'patient1@clinic.com', '01000000003', '$2a$10$m/DF4yUfV3DFpU3DQ78I3eHQQxEEwhoMIJgkglm0F0PhPONtowYpu', 'PATIENT');
SET @pat1 = LAST_INSERT_ID();

INSERT INTO users (name, email, phone, password, role) VALUES
('Ahmed Farouk', 'patient2@clinic.com', '01000000004', '$2a$10$m/DF4yUfV3DFpU3DQ78I3eHQQxEEwhoMIJgkglm0F0PhPONtowYpu', 'PATIENT');
SET @pat2 = LAST_INSERT_ID();

-- ---------- Doctors (JOINED inheritance -> same id as users) ----------
INSERT INTO doctors (id, specialty, location, clinic, experience, consultation_fee, rating, approved) VALUES
(@doc1, 'Cardiology',  'Nasr City, Cairo', 'Cairo Heart Clinic', 12, 350.00, 4.8, 1),
(@doc2, 'Dermatology', 'Maadi, Cairo',    'Skin Care Center',   6,  250.00, 4.2, 0);  -- pending approval: test /admin/doctors

-- ---------- Patients ----------
INSERT INTO patients (id, blood_type, gender, date_of_birth) VALUES
(@pat1, 'O+', 'F', '1992-03-15'),
(@pat2, 'A-', 'M', '1985-11-02');

-- ---------- Schedules ----------
INSERT INTO schedules (doctor_id, available_date, start_time, end_time, booked, version) VALUES
(@doc1, DATE_ADD(CURDATE(), INTERVAL 1 DAY),  '09:00:00', '09:30:00', 1, 0);
SET @s1 = LAST_INSERT_ID();

INSERT INTO schedules (doctor_id, available_date, start_time, end_time, booked, version) VALUES
(@doc1, DATE_ADD(CURDATE(), INTERVAL 1 DAY),  '10:00:00', '10:30:00', 1, 0);
SET @s2 = LAST_INSERT_ID();

INSERT INTO schedules (doctor_id, available_date, start_time, end_time, booked, version) VALUES
(@doc1, DATE_ADD(CURDATE(), INTERVAL 1 DAY),  '11:00:00', '11:30:00', 0, 0);  -- free slot -> test booking
SET @s3 = LAST_INSERT_ID();

INSERT INTO schedules (doctor_id, available_date, start_time, end_time, booked, version) VALUES
(@doc1, DATE_ADD(CURDATE(), INTERVAL 3 DAY),  '09:00:00', '09:30:00', 0, 0);
INSERT INTO schedules (doctor_id, available_date, start_time, end_time, booked, version) VALUES
(@doc1, DATE_ADD(CURDATE(), INTERVAL 7 DAY),  '15:00:00', '15:30:00', 0, 0);
INSERT INTO schedules (doctor_id, available_date, start_time, end_time, booked, version) VALUES
(@doc2, DATE_ADD(CURDATE(), INTERVAL 1 DAY),  '09:00:00', '09:30:00', 1, 0);
SET @s6 = LAST_INSERT_ID();

INSERT INTO schedules (doctor_id, available_date, start_time, end_time, booked, version) VALUES
(@doc1, DATE_ADD(CURDATE(), INTERVAL -1 DAY), '14:00:00', '14:30:00', 1, 0);  -- past visit (completed)
SET @s7 = LAST_INSERT_ID();

-- ---------- Appointments ----------
INSERT INTO appointments (patient_id, doctor_id, schedule_id, status, reminder_sent, notes, created_at) VALUES
(@pat1, @doc1, @s1, 'PENDING',   0, 'Chest pain since last week', NOW());
SET @a1 = LAST_INSERT_ID();

INSERT INTO appointments (patient_id, doctor_id, schedule_id, status, reminder_sent, notes, created_at) VALUES
(@pat2, @doc1, @s2, 'CONFIRMED', 0, 'Follow-up visit',           NOW());
INSERT INTO appointments (patient_id, doctor_id, schedule_id, status, reminder_sent, notes, created_at) VALUES
(@pat1, @doc1, @s7, 'COMPLETED', 1, 'Routine check-up',          DATE_SUB(NOW(), INTERVAL 5 DAY));
SET @a3 = LAST_INSERT_ID();

INSERT INTO appointments (patient_id, doctor_id, schedule_id, status, reminder_sent, notes, created_at) VALUES
(@pat2, @doc2, @s6, 'PENDING',   0, 'Skin rash consultation',    NOW());

-- ---------- Medical records ----------
INSERT INTO medical_records (patient_id, doctor_id, appointment_id, diagnosis, prescription, notes, created_at) VALUES
(@pat1, @doc1, @a3, 'Mild hypertension', 'Amlodipine 5mg once daily', 'Monitor BP weekly', DATE_SUB(NOW(), INTERVAL 5 DAY));

-- ---------- Sample queries ----------
SELECT u.id, u.name, u.email, u.role, d.specialty, d.approved
FROM users u JOIN doctors d ON d.id = u.id;

SELECT s.id, u.name AS doctor, s.available_date, s.start_time, s.end_time, s.booked
FROM schedules s JOIN doctors d ON d.id = s.doctor_id
                 JOIN users u ON u.id = d.id
ORDER BY s.available_date, s.start_time;

SELECT a.id, pu.name AS patient, du.name AS doctor, a.status, a.notes
FROM appointments a
JOIN users pu ON pu.id = a.patient_id
JOIN users du ON du.id = a.doctor_id;

ALTER TABLE patients DROP FOREIGN KEY n8xphvlp05nd3ydg0p1rbdaom,
                     ADD CONSTRAINT n8xphvlp05nd3ydg0p1rbdaom FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE doctors DROP FOREIGN KEY gisys6qm9qflq8w4npdhxafne,
                    ADD CONSTRAINT gisys6qm9qflq8w4npdhxafne FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE appointments DROP FOREIGN KEY 8exap5wmg8kmb1g1rx3by21yt,
                         ADD CONSTRAINT 8exap5wmg8kmb1g1rx3by21yt FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE;
ALTER TABLE appointments DROP FOREIGN KEY mujeo4tymoo98cmf7uj3vsv76,
                         ADD CONSTRAINT mujeo4tymoo98cmf7uj3vsv76 FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE;
ALTER TABLE appointments DROP FOREIGN KEY 20g4fjnwy7g8i5yt9vc1kr923,
                         ADD CONSTRAINT 20g4fjnwy7g8i5yt9vc1kr923 FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE CASCADE;
ALTER TABLE schedules DROP FOREIGN KEY fpyatautb52nts46e1y1y4nvg,
                      ADD CONSTRAINT fpyatautb52nts46e1y1y4nvg FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE;
ALTER TABLE medical_records DROP FOREIGN KEY rav12h9aiw7pegjt62p8owwn3,
                            ADD CONSTRAINT rav12h9aiw7pegjt62p8owwn3 FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE;
ALTER TABLE medical_records DROP FOREIGN KEY tny13k9v4o58styd47st3s2l5,
                            ADD CONSTRAINT tny13k9v4o58styd47st3s2l5 FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE;
ALTER TABLE medical_records DROP FOREIGN KEY ifeec8p5v06rt258odelw8s7j,
                            ADD CONSTRAINT ifeec8p5v06rt258odelw8s7j FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE;

delete from clinic.users
where users.email IN ('youssefyaseremam@gmail.com','yy1170@fayoum.edu.eg');