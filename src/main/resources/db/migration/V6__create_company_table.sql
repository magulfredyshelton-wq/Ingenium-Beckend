CREATE TABLE company (
    id UUID PRIMARY KEY REFERENCES usuario(id),
    company_name VARCHAR(155) NOT NULL,
    corporate_name VARCHAR(155) NOT NULL,
    activity_area_id UUID REFERENCES activity_area(id)
);