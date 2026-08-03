-- ============================================
-- Seed data for clinic database
-- Password for ALL seeded users below = "password123"
-- BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
-- ============================================

-- ============================================
-- DOCTORS (users + doctors rows)
-- ============================================

INSERT INTO users (name, email, phone, password, role) VALUES
                                                           ('Dr. Ahmed Fathy', 'ahmed.fathy@clinic.com', '01011112222', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'DOCTOR'),
                                                           ('Dr. Mona Adel', 'mona.adel@clinic.com', '01022223333', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'DOCTOR'),
                                                           ('Dr. Karim Sami', 'karim.sami@clinic.com', '01033334444', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'DOCTOR');

-- Grab the generated IDs for the inserts below.
-- Assuming this is a fresh table and these land as IDs 1, 2, 3 —
-- if not, replace with SELECT id FROM users WHERE email = '...'.

INSERT INTO doctors (id, specialty, location, clinic, experience, consultation_fee, rating) VALUES
                                                                                                (1, 'Cardiology', 'Fayoum', 'Al-Fayoum Medical Center', 12, 300.00, 0.0),
                                                                                                (2, 'Dermatology', 'Cairo', 'Nile Skin Clinic', 8, 250.00, 0.0),
                                                                                                (3, 'Pediatrics', 'Fayoum', 'Family Care Clinic', 15, 200.00, 0.0);

-- ============================================
-- PATIENTS (users + patients rows)
-- ============================================

INSERT INTO users (name, email, phone, password, role) VALUES
                                                           ('Youssef Test', 'youssef.patient@clinic.com', '01044445555', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'PATIENT'),
                                                           ('Sara Ibrahim', 'sara.ibrahim@clinic.com', '01055556666', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'PATIENT');

-- Assuming these land as IDs 4 and 5 (continuing after the 3 doctors above)

INSERT INTO patients (id, blood_type, gender, date_of_birth) VALUES
                                                                 (4, 'O+', 'male', '2000-05-14'),
                                                                 (5, 'A-', 'female', '1998-11-02');

-- ============================================
-- SCHEDULES (open slots for the doctors above)
-- ============================================

INSERT INTO schedules (doctor_id, available_date, start_time, end_time, booked, version) VALUES
                                                                                             (1, '2026-07-20', '09:00:00', '09:30:00', false, 0),
                                                                                             (1, '2026-07-20', '09:30:00', '10:00:00', false, 0),
                                                                                             (1, '2026-07-21', '10:00:00', '10:30:00', false, 0),
                                                                                             (2, '2026-07-20', '11:00:00', '11:30:00', false, 0),
                                                                                             (2, '2026-07-22', '14:00:00', '14:30:00', false, 0),
                                                                                             (3, '2026-07-21', '13:00:00', '13:30:00', false, 0);