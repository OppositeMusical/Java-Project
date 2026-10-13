INSERT INTO projects (name, description, created_at) VALUES
    ('Website Redesign', 'Refresh the marketing site with the new brand.', CURRENT_TIMESTAMP),
    ('Mobile App', 'First release of the iOS and Android apps.', CURRENT_TIMESTAMP);

INSERT INTO tasks (title, description, status, priority, due_date, project_id, created_at, updated_at) VALUES
    ('Draft wireframes', 'Home, pricing and contact pages.', 'DONE', 'HIGH', NULL,
        (SELECT id FROM projects WHERE name = 'Website Redesign'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Build component library', NULL, 'IN_PROGRESS', 'MEDIUM', NULL,
        (SELECT id FROM projects WHERE name = 'Website Redesign'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Set up analytics', NULL, 'TODO', 'LOW', NULL,
        (SELECT id FROM projects WHERE name = 'Website Redesign'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Design login flow', NULL, 'TODO', 'HIGH', NULL,
        (SELECT id FROM projects WHERE name = 'Mobile App'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
