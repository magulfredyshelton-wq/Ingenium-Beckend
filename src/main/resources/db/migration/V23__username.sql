-- Nome de utilizador (@username): único, em minúsculas, 3 a 30 caracteres.
ALTER TABLE usuario ADD COLUMN username varchar(30);

-- contas que já existam (as de teste da V15) recebem um username automático
UPDATE usuario
SET username = 'u' || substr(md5(id::text), 1, 10)
WHERE username IS NULL;

-- nomes legíveis para as contas de teste
UPDATE usuario SET username = 'ana_redes'       WHERE id = 'a0000000-0000-0000-0000-000000000001';
UPDATE usuario SET username = 'bruno_info'      WHERE id = 'a0000000-0000-0000-0000-000000000002';
UPDATE usuario SET username = 'carla_contabil'  WHERE id = 'a0000000-0000-0000-0000-000000000003';
UPDATE usuario SET username = 'daniel_semcurso' WHERE id = 'a0000000-0000-0000-0000-000000000004';
UPDATE usuario SET username = 'tecnomoz'        WHERE id = 'c0000000-0000-0000-0000-000000000001';
UPDATE usuario SET username = 'contamais'       WHERE id = 'c0000000-0000-0000-0000-000000000002';

ALTER TABLE usuario ALTER COLUMN username SET NOT NULL;
ALTER TABLE usuario
    ADD CONSTRAINT chk_usuario_username CHECK (username ~ '^[a-z0-9_.]{3,30}$');
ALTER TABLE usuario
    ADD CONSTRAINT uq_usuario_username UNIQUE (username);