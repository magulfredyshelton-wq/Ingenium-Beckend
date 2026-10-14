CREATE TABLE students (
    id UUID PRIMARY KEY REFERENCES usuario(id),
    full_name VARCHAR(155) NOT NULL,
    age SMALLINT NOT NULL,
    course_id UUID REFERENCES course(id)
);