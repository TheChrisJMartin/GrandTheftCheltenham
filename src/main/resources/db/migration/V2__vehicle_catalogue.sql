-- F16-0015 Vehicle Catalogue & Physics Profiles (UK 1995-2010)
-- + signature brown Vauxhall Astra Mk1

INSERT INTO vehicle_models (id, display_name, year_from, year_to, body_style, drivetrain, mass_kg, power_hp, top_speed_kmh, accel_0_100_s, drag_coeff, frontal_area_m2, wheelbase_m, steering_sensitivity, grip_multiplier, drift_threshold, colour, category, tags) VALUES
('vauxhall_astra_mk1_brown', 'Vauxhall Astra Mk1 (Brown)', 1979, 1984, 'hatch', 'FWD', 950, 75, 165, 12.5, 0.38, 2.0, 2.52, 1.1, 0.92, 0.85, '#6B4423', 'car', ARRAY['classic','signature','unlocked_by_mission']),
('ford_fiesta_mk5', 'Ford Fiesta Mk5', 2002, 2008, 'supermini', 'FWD', 950, 90, 170, 12.0, 0.34, 1.9, 2.49, 1.15, 0.95, 0.88, '#C0C0C0', 'car', ARRAY['common']),
('ford_focus_mk1', 'Ford Focus Mk1', 1998, 2004, 'hatch', 'FWD', 1150, 115, 195, 10.0, 0.32, 2.1, 2.615, 1.05, 1.05, 0.92, '#1E3A5F', 'car', ARRAY['common','benchmark']),
('ford_focus_mk2', 'Ford Focus Mk2', 2004, 2010, 'hatch', 'FWD', 1250, 125, 200, 9.5, 0.31, 2.15, 2.64, 1.0, 1.02, 0.93, '#2F4F4F', 'car', ARRAY['common']),
('vauxhall_corsa_c', 'Vauxhall Corsa C', 2000, 2006, 'supermini', 'FWD', 950, 80, 170, 13.0, 0.35, 1.85, 2.49, 1.1, 0.9, 0.87, '#FF0000', 'car', ARRAY['common']),
('vauxhall_astra_mk4', 'Vauxhall Astra Mk4', 1998, 2004, 'hatch', 'FWD', 1150, 100, 195, 11.0, 0.33, 2.1, 2.61, 1.0, 0.98, 0.9, '#4169E1', 'car', ARRAY['common']),
('vauxhall_astra_mk5', 'Vauxhall Astra Mk5', 2004, 2010, 'hatch', 'FWD', 1250, 140, 210, 9.0, 0.30, 2.2, 2.685, 1.0, 1.0, 0.91, '#000080', 'car', ARRAY['common']),
('vw_golf_mk5', 'VW Golf Mk5', 2003, 2008, 'hatch', 'FWD', 1200, 115, 200, 10.0, 0.32, 2.15, 2.578, 1.0, 1.0, 0.94, '#708090', 'car', ARRAY['common','refined']),
('peugeot_206', 'Peugeot 206', 1998, 2009, 'supermini', 'FWD', 1000, 90, 180, 11.5, 0.34, 1.95, 2.44, 1.1, 0.97, 0.89, '#FFD700', 'car', ARRAY['common','fun']),
('bmw_3_e46', 'BMW 3 Series E46', 1998, 2006, 'saloon', 'RWD', 1400, 170, 230, 8.0, 0.29, 2.2, 2.725, 0.95, 1.1, 0.98, '#000000', 'car', ARRAY['premium','rwd']),
('mini_r56', 'MINI Cooper R56', 2006, 2010, 'supermini', 'FWD', 1150, 120, 200, 9.0, 0.35, 1.9, 2.467, 1.2, 1.05, 0.9, '#C41E3A', 'car', ARRAY['go-kart']),
('nissan_qashqai', 'Nissan Qashqai', 2007, 2013, 'crossover', 'FWD', 1400, 115, 190, 11.0, 0.35, 2.4, 2.63, 0.9, 0.95, 0.85, '#556B2F', 'car', ARRAY['crossover']),
('renault_clio_182', 'Renault Clio 182', 2004, 2005, 'supermini', 'FWD', 1050, 180, 215, 7.5, 0.34, 1.95, 2.47, 1.15, 1.05, 0.95, '#FF4500', 'car', ARRAY['hot-hatch']),
('honda_civic_ep3', 'Honda Civic Type R EP3', 2001, 2005, 'hatch', 'FWD', 1200, 200, 225, 6.8, 0.33, 2.1, 2.57, 1.1, 1.08, 0.96, '#FFD700', 'car', ARRAY['type-r'])
ON CONFLICT (id) DO NOTHING;

INSERT INTO vehicle_models (id, display_name, year_from, year_to, body_style, drivetrain, mass_kg, power_hp, top_speed_kmh, accel_0_100_s, drag_coeff, frontal_area_m2, wheelbase_m, steering_sensitivity, grip_multiplier, drift_threshold, colour, category, subcategory, tags) VALUES
('delivery_transit', 'Ford Transit Mk6', 2000, 2006, 'van', 'FWD', 2300, 130, 145, 14.0, 0.40, 3.5, 3.3, 0.7, 0.85, 0.7, '#FFFFFF', 'utility', 'delivery', ARRAY['van','stealable','traffic']),
('delivery_sprinter', 'Mercedes Sprinter', 2000, 2010, 'van', 'RWD', 2500, 140, 150, 13.5, 0.38, 3.8, 3.25, 0.7, 0.85, 0.7, '#FFFFFF', 'utility', 'delivery', ARRAY['van','stealable']),
('ambulance_sprinter', 'Ambulance (Sprinter)', 2000, 2010, 'van', 'RWD', 3000, 150, 140, 15.0, 0.40, 4.0, 3.25, 0.65, 0.8, 0.65, '#FFFF00', 'emergency', 'ambulance', ARRAY['emergency','blue-lights']),
('refuse_truck', 'Refuse Collection Vehicle', 1995, 2010, 'truck', 'RWD', 13000, 280, 90, 25.0, 0.55, 6.0, 5.0, 0.4, 0.7, 0.5, '#808080', 'utility', 'municipal', ARRAY['refuse','heavy']),
('taxi_tx', 'LTI TXII Black Cab', 2002, 2010, 'taxi', 'RWD', 1800, 100, 140, 15.0, 0.40, 2.8, 3.0, 0.8, 0.9, 0.75, '#000000', 'utility', 'taxi', ARRAY['taxi','black-cab']),
('police_astra', 'Police Vauxhall Astra', 2004, 2010, 'hatch', 'FWD', 1250, 140, 210, 9.0, 0.30, 2.2, 2.685, 1.0, 1.05, 0.92, '#0000FF', 'police', 'irv', ARRAY['police','ax']),
('police_focus', 'Police Ford Focus', 2004, 2010, 'hatch', 'FWD', 1250, 130, 200, 9.5, 0.31, 2.15, 2.64, 1.0, 1.05, 0.92, '#0000FF', 'police', 'irv', ARRAY['police','ax'])
ON CONFLICT (id) DO NOTHING;

UPDATE vehicle_models SET is_police = TRUE WHERE category = 'police';
UPDATE vehicle_models SET physics_profile = jsonb_build_object(
  'maxSteerRad', 0.6,
  'driveForce', power_hp * 12.0,
  'brakeForce', mass_kg * 8.0,
  'rollResistance', 0.015
);
