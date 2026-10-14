-- R__dev_test_data_feed.sql
-- TEST DATA ONLY. Repeatable Flyway migration, loaded only by the "dev" profile
-- (folder db/seed-dev). Never put it in db/migration.
-- Safe to run again: it deletes the test rows first (fixed ids).
-- ASCII only on purpose, so the Windows terminal does not garble anything.
--
-- Students:  A = Tecnico de Administracao de Redes (seeking INTERNSHIP)
--            B = Engenharia Informatica            (seeking BOTH)
--            C = Contabilidade                     (seeking JOB)
--            D = no course, no tags                (seeking BOTH)
-- Companies: C1 = TecnoMoz (tech)   C2 = ContaMais (management)

-- =========================================================
-- 0. Cleanup of previous test rows
-- =========================================================
DELETE FROM post WHERE author_id IN (
                                     'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002',
                                     'a0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000004',
                                     'c0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002');
DELETE FROM students WHERE id IN (
                                  'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002',
                                  'a0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000004');
DELETE FROM company WHERE id IN (
                                 'c0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002');
DELETE FROM usuario WHERE id IN (
                                 'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002',
                                 'a0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000004',
                                 'c0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000002');

-- =========================================================
-- 1. Users (fake password: these accounts cannot log in)
-- =========================================================
INSERT INTO usuario (id, email, password, user_type) VALUES
                                                         ('a0000000-0000-0000-0000-000000000001', 'teste.redes@ingenium.test',        'SEM_LOGIN', 'STUDENT'),
                                                         ('a0000000-0000-0000-0000-000000000002', 'teste.informatica@ingenium.test',  'SEM_LOGIN', 'STUDENT'),
                                                         ('a0000000-0000-0000-0000-000000000003', 'teste.contabilidade@ingenium.test','SEM_LOGIN', 'STUDENT'),
                                                         ('a0000000-0000-0000-0000-000000000004', 'teste.semcurso@ingenium.test',     'SEM_LOGIN', 'STUDENT'),
                                                         ('c0000000-0000-0000-0000-000000000001', 'teste.tecnomoz@ingenium.test',     'SEM_LOGIN', 'COMPANY'),
                                                         ('c0000000-0000-0000-0000-000000000002', 'teste.contamais@ingenium.test',    'SEM_LOGIN', 'COMPANY');

INSERT INTO students (id, full_name, course_id, birth_date, seeking) VALUES
                                                                         ('a0000000-0000-0000-0000-000000000001', 'Ana Redes',         '3a2a9c3f-a540-43f5-9ebd-cbb527515b5d', '2003-05-10', 'INTERNSHIP'),
                                                                         ('a0000000-0000-0000-0000-000000000002', 'Bruno Informatica', '68c25086-09f5-4032-8458-490ef5659df9', '2002-08-21', 'BOTH'),
                                                                         ('a0000000-0000-0000-0000-000000000003', 'Carla Contabil',    'd21e7edd-471a-4419-9ccc-2bf2bc8d68d3', '2001-02-14', 'JOB'),
                                                                         ('a0000000-0000-0000-0000-000000000004', 'Daniel Sem Curso',  NULL,                                   '2004-11-03', 'BOTH');

INSERT INTO company (id, company_name, corporate_name, activity_area_id) VALUES
                                                                             ('c0000000-0000-0000-0000-000000000001', 'TecnoMoz',  'TecnoMoz Lda',  'bfca158d-e83f-4d30-85b7-105075cb1262'),
                                                                             ('c0000000-0000-0000-0000-000000000002', 'ContaMais', 'ContaMais SA',  '9e703b09-c01e-4928-8af2-ac56f1bfa5c3');

-- =========================================================
-- 2. Posts (supertype). Ids: f...01-07 jobs, 11-13 projects, 21-24 general
-- =========================================================
INSERT INTO post (id, author_id, post_type, created_at) VALUES
                                                            ('f0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001', 'JOB',     now() - interval '2 days'),
                                                            ('f0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001', 'JOB',     now() - interval '1 day'),
                                                            ('f0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001', 'JOB',     now() - interval '6 hours'),
                                                            ('f0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001', 'JOB',     now() - interval '10 days'),
                                                            ('f0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000001', 'JOB',     now() - interval '4 days'),
                                                            ('f0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000002', 'JOB',     now() - interval '3 days'),
                                                            ('f0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000002', 'JOB',     now() - interval '2 days'),
                                                            ('f0000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000002', 'PROJECT', now() - interval '1 day'),
                                                            ('f0000000-0000-0000-0000-000000000012', 'a0000000-0000-0000-0000-000000000001', 'PROJECT', now() - interval '3 days'),
                                                            ('f0000000-0000-0000-0000-000000000013', 'a0000000-0000-0000-0000-000000000003', 'PROJECT', now() - interval '2 days'),
                                                            ('f0000000-0000-0000-0000-000000000021', 'a0000000-0000-0000-0000-000000000002', 'GENERAL', now() - interval '5 hours'),
                                                            ('f0000000-0000-0000-0000-000000000022', 'a0000000-0000-0000-0000-000000000001', 'GENERAL', now() - interval '2 days'),
                                                            ('f0000000-0000-0000-0000-000000000023', 'c0000000-0000-0000-0000-000000000001', 'GENERAL', now() - interval '12 hours'),
                                                            ('f0000000-0000-0000-0000-000000000024', 'a0000000-0000-0000-0000-000000000004', 'GENERAL', now() - interval '8 hours');

-- Jobs. 04 is expired by date (still OPEN on purpose), 05 is CLOSED.
INSERT INTO job (post_id, company_id, title, description, work_type,
                 application_start, application_end, start_date, end_date, status) VALUES
                                                                                       ('f0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001',
                                                                                        'Estagio em Redes', 'Estagio na equipa de infraestrutura.', 'INTERNSHIP',
                                                                                        CURRENT_DATE - 5, CURRENT_DATE + 5, CURRENT_DATE + 30, CURRENT_DATE + 120, 'OPEN'),
                                                                                       ('f0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001',
                                                                                        'Programador Mobile Junior', 'Desenvolvimento de apps moveis.', 'JOB',
                                                                                        CURRENT_DATE - 1, CURRENT_DATE + 20, CURRENT_DATE + 40, NULL, 'OPEN'),
                                                                                       ('f0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000001',
                                                                                        'Estagio em Programacao Web', 'Estagio em desenvolvimento web (fecha em breve).', 'INTERNSHIP',
                                                                                        CURRENT_DATE - 1, CURRENT_DATE + 3, CURRENT_DATE + 30, CURRENT_DATE + 90, 'OPEN'),
                                                                                       ('f0000000-0000-0000-0000-000000000004', 'c0000000-0000-0000-0000-000000000001',
                                                                                        'Tecnico de Redes (EXPIRADA)', 'Prazo ja terminou.', 'JOB',
                                                                                        CURRENT_DATE - 13, CURRENT_DATE - 3, NULL, NULL, 'OPEN'),
                                                                                       ('f0000000-0000-0000-0000-000000000005', 'c0000000-0000-0000-0000-000000000001',
                                                                                        'Analista de Dados (FECHADA)', 'Vaga fechada pela empresa.', 'JOB',
                                                                                        CURRENT_DATE - 4, CURRENT_DATE + 10, NULL, NULL, 'CLOSED'),
                                                                                       ('f0000000-0000-0000-0000-000000000006', 'c0000000-0000-0000-0000-000000000002',
                                                                                        'Estagio em Contabilidade', 'Estagio no departamento financeiro.', 'INTERNSHIP',
                                                                                        CURRENT_DATE - 3, CURRENT_DATE + 10, CURRENT_DATE + 20, CURRENT_DATE + 110, 'OPEN'),
                                                                                       ('f0000000-0000-0000-0000-000000000007', 'c0000000-0000-0000-0000-000000000002',
                                                                                        'Assistente de RH', 'Apoio ao recrutamento.', 'JOB',
                                                                                        CURRENT_DATE - 2, CURRENT_DATE + 15, NULL, NULL, 'OPEN');

INSERT INTO project (post_id, title, description, repository_url, status) VALUES
                                                                              ('f0000000-0000-0000-0000-000000000011', 'App de transporte', 'App mobile para horarios de machimbombos.', NULL, 'IN_PROGRESS'),
                                                                              ('f0000000-0000-0000-0000-000000000012', 'Laboratorio de VLANs', 'Simulacao de VLANs no Packet Tracer.', NULL, 'COMPLETED'),
                                                                              ('f0000000-0000-0000-0000-000000000013', 'Folha de calculo de IVA', 'Modelo em Excel para apurar o IVA.', NULL, 'COMPLETED');

INSERT INTO general_post (post_id, content) VALUES
                                                ('f0000000-0000-0000-0000-000000000021', 'Alguem quer estudar inteligencia artificial em grupo?'),
                                                ('f0000000-0000-0000-0000-000000000022', 'Dica: nunca deixem a senha padrao do router.'),
                                                ('f0000000-0000-0000-0000-000000000023', 'Estamos a contratar, fiquem atentos as vagas!'),
                                                ('f0000000-0000-0000-0000-000000000024', 'Alguem conhece bons livros de enfermagem?');

-- =========================================================
-- 3. Tags of each post (tag found by slug)
-- =========================================================
INSERT INTO post_tag (post_id, tag_id)
SELECT v.post_id::uuid, t.id
FROM (VALUES
          ('f0000000-0000-0000-0000-000000000001', 'redes'),
          ('f0000000-0000-0000-0000-000000000001', 'vlan'),
          ('f0000000-0000-0000-0000-000000000002', 'appmobile'),
          ('f0000000-0000-0000-0000-000000000002', 'programacao'),
          ('f0000000-0000-0000-0000-000000000003', 'programacao'),
          ('f0000000-0000-0000-0000-000000000003', 'desenvolvimento-web'),
          ('f0000000-0000-0000-0000-000000000003', 'bases-de-dados'),
          ('f0000000-0000-0000-0000-000000000004', 'redes'),
          ('f0000000-0000-0000-0000-000000000005', 'bases-de-dados'),
          ('f0000000-0000-0000-0000-000000000006', 'contabilidade'),
          ('f0000000-0000-0000-0000-000000000006', 'excel'),
          ('f0000000-0000-0000-0000-000000000006', 'fiscalidade'),
          ('f0000000-0000-0000-0000-000000000007', 'recursos-humanos'),
          ('f0000000-0000-0000-0000-000000000007', 'recrutamento'),
          ('f0000000-0000-0000-0000-000000000011', 'appmobile'),
          ('f0000000-0000-0000-0000-000000000011', 'programacao'),
          ('f0000000-0000-0000-0000-000000000011', 'bases-de-dados'),
          ('f0000000-0000-0000-0000-000000000012', 'redes'),
          ('f0000000-0000-0000-0000-000000000012', 'vlan'),
          ('f0000000-0000-0000-0000-000000000013', 'excel'),
          ('f0000000-0000-0000-0000-000000000013', 'fiscalidade'),
          ('f0000000-0000-0000-0000-000000000013', 'contabilidade'),
          ('f0000000-0000-0000-0000-000000000021', 'inteligencia-artificial'),
          ('f0000000-0000-0000-0000-000000000022', 'ciberseguranca'),
          ('f0000000-0000-0000-0000-000000000022', 'redes'),
          ('f0000000-0000-0000-0000-000000000023', 'programacao'),
          ('f0000000-0000-0000-0000-000000000024', 'enfermagem-clinica')
     ) AS v(post_id, slug)
         JOIN tag t ON t.slug = v.slug;

-- =========================================================
-- 4. Who follows whom
--    A follows B and TecnoMoz; B follows A and TecnoMoz; C follows ContaMais; D follows nobody
-- =========================================================
INSERT INTO follower (follower_id, followed_id) VALUES
                                                    ('a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002'),
                                                    ('a0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001'),
                                                    ('a0000000-0000-0000-0000-000000000001', 'c0000000-0000-0000-0000-000000000001'),
                                                    ('a0000000-0000-0000-0000-000000000002', 'c0000000-0000-0000-0000-000000000001'),
                                                    ('a0000000-0000-0000-0000-000000000003', 'c0000000-0000-0000-0000-000000000002');

-- =========================================================
-- 5. Tags and categories followed by users
--    A follows #ciberseguranca; B follows #inteligencia-artificial; C follows the whole TI category
-- =========================================================
INSERT INTO user_tag (user_id, tag_id)
SELECT v.user_id::uuid, t.id
FROM (VALUES
          ('a0000000-0000-0000-0000-000000000001', 'ciberseguranca'),
          ('a0000000-0000-0000-0000-000000000002', 'inteligencia-artificial')
     ) AS v(user_id, slug)
         JOIN tag t ON t.slug = v.slug;

INSERT INTO user_tag_category (user_id, tag_category_id)
SELECT 'a0000000-0000-0000-0000-000000000003'::uuid, id FROM tag_category WHERE name = 'TI';

-- =========================================================
-- 6. Quick check (expected: 6 users, 14 posts, 7 jobs, 27 post_tag, 5 followers, 2 user_tag, 1 category)
-- =========================================================
SELECT
    (SELECT count(*) FROM usuario  WHERE email LIKE 'teste.%@ingenium.test') AS users,
    (SELECT count(*) FROM post     WHERE id::text LIKE 'f0000000%')          AS posts,
  (SELECT count(*) FROM job      WHERE post_id::text LIKE 'f0000000%')     AS jobs,
  (SELECT count(*) FROM post_tag WHERE post_id::text LIKE 'f0000000%')     AS post_tags,
  (SELECT count(*) FROM follower WHERE follower_id::text LIKE 'a0000000%') AS followers,
  (SELECT count(*) FROM user_tag WHERE user_id::text LIKE 'a0000000%')     AS user_tags,
  (SELECT count(*) FROM user_tag_category WHERE user_id::text LIKE 'a0000000%') AS user_categories;