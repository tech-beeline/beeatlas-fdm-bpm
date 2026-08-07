ALTER TABLE processes.application
    ADD CONSTRAINT uk_application_business_key UNIQUE (business_key);