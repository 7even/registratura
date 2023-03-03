CREATE INDEX ts_idx ON patients USING GIN (
    to_tsvector('english',
                coalesce(first_name, '') ||
                ' ' ||
                coalesce(middle_name, '') ||
                ' ' ||
                coalesce(last_name, '') ||
                ' ' ||
                coalesce(address, '') ||
                ' ' ||
                coalesce(insurance_number, '')
    )
);
