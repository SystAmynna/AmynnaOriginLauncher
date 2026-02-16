
INSERT INTO Files (path, hash, size, name, description) VALUES
('mods/create-1.20.1-6.0.8.jar', NULL, NULL, 'Create', NULL),
('mods/createdeco-2.0.3-1.20.1-forge.jar', NULL, NULL, 'Create Deco', 'Décorations pour le mod Create'),
('mods/xray-1.20.1-1.0.0.jar', NULL, NULL, 'XRay', 'CHEAT (test)'),
('mods/jei-1.20.1-11.3.0.jar', NULL, NULL, 'Just Enough Items', 'Affiche les recettes de tous les items du jeu');

INSERT INTO Dependencies (file, dependency) VALUES
('mods/createdeco-2.0.3-1.20.1-forge.jar', 'mods/create-1.20.1-6.0.8.jar');

INSERT INTO ToInstall (file, optional, admin_required) VALUES
('mods/create-1.20.1-6.0.8.jar', FALSE, FALSE),
('mods/createdeco-2.0.3-1.20.1-forge.jar', TRUE, FALSE),
('mods/xray-1.20.1-1.0.0.jar', TRUE, TRUE),
('mods/jei-1.20.1-11.3.0.jar', FALSE, TRUE);

