CREATE TABLE course (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    field_study_id UUID REFERENCES field_study(id)
);