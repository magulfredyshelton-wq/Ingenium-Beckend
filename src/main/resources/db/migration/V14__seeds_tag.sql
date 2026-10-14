
UPDATE activity_area SET field_study_id = 'ba128c60-2964-45ec-b685-95f270e6cf12' WHERE id = 'bfca158d-e83f-4d30-85b7-105075cb1262'; -- Engenharia & Tecnologia
UPDATE activity_area SET field_study_id = 'a2dfd255-6658-45cd-9534-609a5531a770' WHERE id = '9e703b09-c01e-4928-8af2-ac56f1bfa5c3'; -- Gestao & Negocios
UPDATE activity_area SET field_study_id = 'abb09b48-7080-4efe-b511-60d764b35d88' WHERE id = '936f18b5-4ac1-4481-a8b9-97d3f1042b3f'; -- Saude & Biologia
UPDATE activity_area SET field_study_id = '1fc844fd-0beb-4541-b88f-11642464df63' WHERE id = '15441851-a042-418a-82f6-b25040fa7824'; -- Ciencias Sociais & Juridica
UPDATE activity_area SET field_study_id = '50b0454f-5ac4-4a17-860d-2398a5c8d479' WHERE id = '43131273-b3fc-467e-83b3-5e1a93d0b400'; -- Educacao e Humanidade

INSERT INTO tag_category (name, field_study_id) VALUES
                                                    ('TI',                'ba128c60-2964-45ec-b685-95f270e6cf12'),
                                                    ('Eletricidade',      'ba128c60-2964-45ec-b685-95f270e6cf12'),
                                                    ('Construção Civil',  'ba128c60-2964-45ec-b685-95f270e6cf12'),
                                                    ('Saúde',             'abb09b48-7080-4efe-b511-60d764b35d88'),
                                                    ('Biologia',          'abb09b48-7080-4efe-b511-60d764b35d88'),
                                                    ('Gestão e Finanças', 'a2dfd255-6658-45cd-9534-609a5531a770');


INSERT INTO tag (tag_category_id, name, slug)
SELECT c.id, v.name, v.slug
FROM (VALUES
          ('TI', 'programação',            'programacao'),
          ('TI', 'desenvolvimento-web',    'desenvolvimento-web'),
          ('TI', 'appmobile',              'appmobile'),
          ('TI', 'bases-de-dados',         'bases-de-dados'),
          ('TI', 'redes',                  'redes'),
          ('TI', 'vlan',                   'vlan'),
          ('TI', 'cibersegurança',         'ciberseguranca'),
          ('TI', 'inteligência-artificial','inteligencia-artificial'),

          ('Eletricidade', 'circuitos',              'circuitos'),
          ('Eletricidade', 'eletrónica',             'eletronica'),
          ('Eletricidade', 'instalações-elétricas',  'instalacoes-eletricas'),
          ('Eletricidade', 'automação',              'automacao'),
          ('Eletricidade', 'energias-renováveis',    'energias-renovaveis'),

          ('Construção Civil', 'estruturas',               'estruturas'),
          ('Construção Civil', 'topografia',               'topografia'),
          ('Construção Civil', 'projeto-de-obras',         'projeto-de-obras'),
          ('Construção Civil', 'materiais-de-construção',  'materiais-de-construcao'),
          ('Construção Civil', 'fiscalização-de-obras',    'fiscalizacao-de-obras'),
          ('Construção Civil', 'autocad',                  'autocad'),

          ('Saúde', 'saúde-pública',       'saude-publica'),
          ('Saúde', 'enfermagem-clínica',  'enfermagem-clinica'),
          ('Saúde', 'farmacologia',        'farmacologia'),
          ('Saúde', 'nutrição-clínica',    'nutricao-clinica'),
          ('Saúde', 'odontologia',         'odontologia'),
          ('Saúde', 'análises-clínicas',   'analises-clinicas'),

          ('Biologia', 'biologia-molecular',       'biologia-molecular'),
          ('Biologia', 'ecologia',                 'ecologia'),
          ('Biologia', 'microbiologia',            'microbiologia'),
          ('Biologia', 'investigação-científica',  'investigacao-cientifica'),

          ('Gestão e Finanças', 'contabilidade',      'contabilidade'),
          ('Gestão e Finanças', 'auditoria',          'auditoria'),
          ('Gestão e Finanças', 'fiscalidade',        'fiscalidade'),
          ('Gestão e Finanças', 'gestão-financeira',  'gestao-financeira'),
          ('Gestão e Finanças', 'recursos-humanos',   'recursos-humanos'),
          ('Gestão e Finanças', 'recrutamento',       'recrutamento'),
          ('Gestão e Finanças', 'excel',              'excel'),
          ('Gestão e Finanças', 'empreendedorismo',   'empreendedorismo')
     ) AS v(category, name, slug)
         JOIN tag_category c ON c.name = v.category;


INSERT INTO course_tag (course_id, tag_id, weight)
SELECT v.course_id::uuid, t.id, v.weight::numeric
FROM (VALUES
          -- Engenharia Informática
          ('68c25086-09f5-4032-8458-490ef5659df9', 'programacao',             1.00),
          ('68c25086-09f5-4032-8458-490ef5659df9', 'desenvolvimento-web',     0.80),
          ('68c25086-09f5-4032-8458-490ef5659df9', 'appmobile',               1.00),
          ('68c25086-09f5-4032-8458-490ef5659df9', 'bases-de-dados',          0.90),
          ('68c25086-09f5-4032-8458-490ef5659df9', 'redes',                   0.40),
          ('68c25086-09f5-4032-8458-490ef5659df9', 'vlan',                    0.30),
          ('68c25086-09f5-4032-8458-490ef5659df9', 'ciberseguranca',          0.60),
          ('68c25086-09f5-4032-8458-490ef5659df9', 'inteligencia-artificial', 0.80),

          -- Técnico Programação Web (sem appmobile)
          ('b0781d33-b343-4b6c-93c5-87561895b132', 'programacao',         1.00),
          ('b0781d33-b343-4b6c-93c5-87561895b132', 'desenvolvimento-web', 1.00),
          ('b0781d33-b343-4b6c-93c5-87561895b132', 'bases-de-dados',      0.70),

          -- Técnico de Administração de Redes (só redes e vlan)
          ('3a2a9c3f-a540-43f5-9ebd-cbb527515b5d', 'redes', 1.00),
          ('3a2a9c3f-a540-43f5-9ebd-cbb527515b5d', 'vlan',  1.00),

          -- Engenharia Eléctrica
          ('362a508a-2ff5-4351-a44d-e1a00f1d6385', 'circuitos',             1.00),
          ('362a508a-2ff5-4351-a44d-e1a00f1d6385', 'eletronica',            1.00),
          ('362a508a-2ff5-4351-a44d-e1a00f1d6385', 'instalacoes-eletricas', 0.80),
          ('362a508a-2ff5-4351-a44d-e1a00f1d6385', 'automacao',             0.90),
          ('362a508a-2ff5-4351-a44d-e1a00f1d6385', 'energias-renovaveis',   0.80),

          -- Técnico Eléctrico
          ('6b2c93ae-8f51-4245-92bf-cefc2f2527a9', 'instalacoes-eletricas', 1.00),
          ('6b2c93ae-8f51-4245-92bf-cefc2f2527a9', 'circuitos',             0.90),
          ('6b2c93ae-8f51-4245-92bf-cefc2f2527a9', 'eletronica',            0.60),
          ('6b2c93ae-8f51-4245-92bf-cefc2f2527a9', 'automacao',             0.60),
          ('6b2c93ae-8f51-4245-92bf-cefc2f2527a9', 'energias-renovaveis',   0.50),

          -- Engenharia Civil
          ('887fcfc6-7758-4f03-a604-20f21308ae88', 'estruturas',              1.00),
          ('887fcfc6-7758-4f03-a604-20f21308ae88', 'projeto-de-obras',        1.00),
          ('887fcfc6-7758-4f03-a604-20f21308ae88', 'materiais-de-construcao', 0.90),
          ('887fcfc6-7758-4f03-a604-20f21308ae88', 'topografia',              0.80),
          ('887fcfc6-7758-4f03-a604-20f21308ae88', 'fiscalizacao-de-obras',   0.80),
          ('887fcfc6-7758-4f03-a604-20f21308ae88', 'autocad',                 0.80),

          -- Técnico Construção Civil
          ('c8466a39-dc2b-442c-8679-5769befe18c3', 'fiscalizacao-de-obras',   1.00),
          ('c8466a39-dc2b-442c-8679-5769befe18c3', 'materiais-de-construcao', 1.00),
          ('c8466a39-dc2b-442c-8679-5769befe18c3', 'projeto-de-obras',        0.90),
          ('c8466a39-dc2b-442c-8679-5769befe18c3', 'topografia',              0.70),
          ('c8466a39-dc2b-442c-8679-5769befe18c3', 'autocad',                 0.70),
          ('c8466a39-dc2b-442c-8679-5769befe18c3', 'estruturas',              0.60),

          -- Medicina
          ('b1eafff6-5732-4169-ab3e-4581d28f1463', 'saude-publica',           1.00),
          ('b1eafff6-5732-4169-ab3e-4581d28f1463', 'farmacologia',            0.70),
          ('b1eafff6-5732-4169-ab3e-4581d28f1463', 'analises-clinicas',       0.70),
          ('b1eafff6-5732-4169-ab3e-4581d28f1463', 'investigacao-cientifica', 0.50),
          ('b1eafff6-5732-4169-ab3e-4581d28f1463', 'biologia-molecular',      0.40),

          -- Enfermagem
          ('895f61c0-8d81-4f2c-8e03-b445031480cf', 'enfermagem-clinica', 1.00),
          ('895f61c0-8d81-4f2c-8e03-b445031480cf', 'saude-publica',      0.80),
          ('895f61c0-8d81-4f2c-8e03-b445031480cf', 'farmacologia',       0.60),
          ('895f61c0-8d81-4f2c-8e03-b445031480cf', 'nutricao-clinica',   0.40),
          ('895f61c0-8d81-4f2c-8e03-b445031480cf', 'analises-clinicas',  0.40),

          -- Estomatologia
          ('f1946ca8-9ab2-4299-abbc-15b2d5e9133d', 'odontologia',       1.00),
          ('f1946ca8-9ab2-4299-abbc-15b2d5e9133d', 'saude-publica',     0.50),
          ('f1946ca8-9ab2-4299-abbc-15b2d5e9133d', 'farmacologia',      0.50),
          ('f1946ca8-9ab2-4299-abbc-15b2d5e9133d', 'analises-clinicas', 0.40),

          -- Biologia
          ('dadbcbaa-dfc7-46cd-9ea6-686c743e961e', 'biologia-molecular',      1.00),
          ('dadbcbaa-dfc7-46cd-9ea6-686c743e961e', 'ecologia',                1.00),
          ('dadbcbaa-dfc7-46cd-9ea6-686c743e961e', 'microbiologia',           1.00),
          ('dadbcbaa-dfc7-46cd-9ea6-686c743e961e', 'investigacao-cientifica', 0.90),
          ('dadbcbaa-dfc7-46cd-9ea6-686c743e961e', 'analises-clinicas',       0.50),

          -- Farmácia
          ('97dccabc-84eb-46bb-8697-3cf364499509', 'farmacologia',       1.00),
          ('97dccabc-84eb-46bb-8697-3cf364499509', 'analises-clinicas',  0.80),
          ('97dccabc-84eb-46bb-8697-3cf364499509', 'microbiologia',      0.50),
          ('97dccabc-84eb-46bb-8697-3cf364499509', 'saude-publica',      0.50),
          ('97dccabc-84eb-46bb-8697-3cf364499509', 'biologia-molecular', 0.40),

          -- Nutrição
          ('583b52bd-d5c7-4321-a973-37071765e2a0', 'nutricao-clinica',        1.00),
          ('583b52bd-d5c7-4321-a973-37071765e2a0', 'saude-publica',           0.70),
          ('583b52bd-d5c7-4321-a973-37071765e2a0', 'analises-clinicas',       0.40),
          ('583b52bd-d5c7-4321-a973-37071765e2a0', 'investigacao-cientifica', 0.40),

          -- Contabilidade
          ('d21e7edd-471a-4419-9ccc-2bf2bc8d68d3', 'contabilidade',     1.00),
          ('d21e7edd-471a-4419-9ccc-2bf2bc8d68d3', 'excel',             1.00),
          ('d21e7edd-471a-4419-9ccc-2bf2bc8d68d3', 'auditoria',         0.90),
          ('d21e7edd-471a-4419-9ccc-2bf2bc8d68d3', 'fiscalidade',       0.90),
          ('d21e7edd-471a-4419-9ccc-2bf2bc8d68d3', 'gestao-financeira', 0.70),

          -- Gestão Financeira e Patrimonial
          ('09597e2b-605e-4078-9734-8c415bcab51a', 'gestao-financeira', 1.00),
          ('09597e2b-605e-4078-9734-8c415bcab51a', 'contabilidade',     0.70),
          ('09597e2b-605e-4078-9734-8c415bcab51a', 'fiscalidade',       0.60),
          ('09597e2b-605e-4078-9734-8c415bcab51a', 'excel',             0.60),
          ('09597e2b-605e-4078-9734-8c415bcab51a', 'auditoria',         0.50),

          -- Gestão de Recursos Humanos
          ('56004ee4-278c-4908-afb2-f38aa047437a', 'recursos-humanos',  1.00),
          ('56004ee4-278c-4908-afb2-f38aa047437a', 'recrutamento',      1.00),
          ('56004ee4-278c-4908-afb2-f38aa047437a', 'excel',             0.50),
          ('56004ee4-278c-4908-afb2-f38aa047437a', 'empreendedorismo',  0.40)
     ) AS v(course_id, slug, weight)
         JOIN tag t ON t.slug = v.slug;