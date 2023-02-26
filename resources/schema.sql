CREATE TYPE patient_gender AS ENUM ('male', 'female');

CREATE TABLE patients (
    id serial PRIMARY KEY,
    first_name varchar NOT NULL,
    middle_name varchar,
    last_name varchar NOT NULL,
    gender patient_gender NOT NULL,
    birthday date NOT NULL,
    address varchar NOT NULL,
    insurance_number varchar NOT NULL
);

CREATE INDEX ts_idx ON patients USING GIN (
    to_tsvector('english',
                coalesce(first_name) ||
                ' ' ||
                coalesce(middle_name) ||
                ' ' ||
                coalesce(last_name) ||
                ' ' ||
                coalesce(address) ||
                ' ' ||
                coalesce(insurance_number)
    )
);
