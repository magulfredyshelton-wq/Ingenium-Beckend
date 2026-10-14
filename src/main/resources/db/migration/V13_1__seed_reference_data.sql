-- Dados de referência que a V14 (tags e course_tag) pressupõe.
-- Os UUIDs são exatamente os que a V14 usa. Os NOMES são palpites tirados dos
-- comentários da V14: corrigir depois com UPDATE se forem diferentes.
-- ON CONFLICT DO NOTHING: não mexe em bases que já tenham estas linhas.

-- Áreas de estudo
INSERT INTO field_study (id, name) VALUES
    ('ba128c60-2964-45ec-b685-95f270e6cf12', 'Engenharia & Tecnologia'),
    ('a2dfd255-6658-45cd-9534-609a5531a770', 'Gestão & Negócios'),
    ('abb09b48-7080-4efe-b511-60d764b35d88', 'Saúde & Biologia'),
    ('1fc844fd-0beb-4541-b88f-11642464df63', 'Ciências Sociais & Jurídica'),
    ('50b0454f-5ac4-4a17-860d-2398a5c8d479', 'Educação e Humanidade')
ON CONFLICT (id) DO NOTHING;

-- Áreas de atividade (a V14 liga cada uma à sua área de estudo)
INSERT INTO activity_area (id, type_activity) VALUES
    ('bfca158d-e83f-4d30-85b7-105075cb1262', 'Engenharia & Tecnologia'),
    ('9e703b09-c01e-4928-8af2-ac56f1bfa5c3', 'Gestão & Negócios'),
    ('936f18b5-4ac1-4481-a8b9-97d3f1042b3f', 'Saúde & Biologia'),
    ('15441851-a042-418a-82f6-b25040fa7824', 'Ciências Sociais & Jurídica'),
    ('43131273-b3fc-467e-83b3-5e1a93d0b400', 'Educação e Humanidade')
ON CONFLICT (id) DO NOTHING;

-- Cursos (a V14 faz course_tag para estes 16 IDs).
-- A área de estudo de cada curso foi deduzida das categorias de tags da V14.
INSERT INTO course (id, name, field_study_id) VALUES
    -- Engenharia & Tecnologia
    ('68c25086-09f5-4032-8458-490ef5659df9', 'Engenharia Informática',            'ba128c60-2964-45ec-b685-95f270e6cf12'),
    ('b0781d33-b343-4b6c-93c5-87561895b132', 'Técnico Programação Web',           'ba128c60-2964-45ec-b685-95f270e6cf12'),
    ('3a2a9c3f-a540-43f5-9ebd-cbb527515b5d', 'Técnico de Administração de Redes', 'ba128c60-2964-45ec-b685-95f270e6cf12'),
    ('362a508a-2ff5-4351-a44d-e1a00f1d6385', 'Engenharia Eléctrica',              'ba128c60-2964-45ec-b685-95f270e6cf12'),
    ('6b2c93ae-8f51-4245-92bf-cefc2f2527a9', 'Técnico Eléctrico',                 'ba128c60-2964-45ec-b685-95f270e6cf12'),
    ('887fcfc6-7758-4f03-a604-20f21308ae88', 'Engenharia Civil',                  'ba128c60-2964-45ec-b685-95f270e6cf12'),
    ('c8466a39-dc2b-442c-8679-5769befe18c3', 'Técnico Construção Civil',          'ba128c60-2964-45ec-b685-95f270e6cf12'),
    -- Saúde & Biologia
    ('b1eafff6-5732-4169-ab3e-4581d28f1463', 'Medicina',                          'abb09b48-7080-4efe-b511-60d764b35d88'),
    ('895f61c0-8d81-4f2c-8e03-b445031480cf', 'Enfermagem',                        'abb09b48-7080-4efe-b511-60d764b35d88'),
    ('f1946ca8-9ab2-4299-abbc-15b2d5e9133d', 'Estomatologia',                     'abb09b48-7080-4efe-b511-60d764b35d88'),
    ('dadbcbaa-dfc7-46cd-9ea6-686c743e961e', 'Biologia',                          'abb09b48-7080-4efe-b511-60d764b35d88'),
    ('97dccabc-84eb-46bb-8697-3cf364499509', 'Farmácia',                          'abb09b48-7080-4efe-b511-60d764b35d88'),
    ('583b52bd-d5c7-4321-a973-37071765e2a0', 'Nutrição',                          'abb09b48-7080-4efe-b511-60d764b35d88'),
    -- Gestão & Negócios
    ('d21e7edd-471a-4419-9ccc-2bf2bc8d68d3', 'Contabilidade',                     'a2dfd255-6658-45cd-9534-609a5531a770'),
    ('09597e2b-605e-4078-9734-8c415bcab51a', 'Gestão Financeira e Patrimonial',   'a2dfd255-6658-45cd-9534-609a5531a770'),
    ('56004ee4-278c-4908-afb2-f38aa047437a', 'Gestão de Recursos Humanos',        'a2dfd255-6658-45cd-9534-609a5531a770')
ON CONFLICT (id) DO NOTHING;
