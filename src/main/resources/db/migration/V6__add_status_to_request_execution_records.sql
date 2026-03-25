ALTER TABLE request_execution_records
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS';

