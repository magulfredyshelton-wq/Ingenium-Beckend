-- "Sobre mim" e contactos do perfil (estudantes e empresas).
ALTER TABLE usuario
    ADD COLUMN bio              varchar(500),
    ADD COLUMN contact_phone    varchar(20),
    ADD COLUMN contact_email    varchar(120),
    ADD COLUMN contact_website  varchar(200),
    ADD COLUMN contact_linkedin varchar(200),
    ADD COLUMN contact_github   varchar(200),
    ADD COLUMN contact_location varchar(100);