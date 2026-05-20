ALTER TABLE merchant ADD CONSTRAINT merchant_status_check
CHECK (merchant_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'));