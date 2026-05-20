ALTER TABLE journal_entry DROP CONSTRAINT journal_entry_entry_type_check;
ALTER TABLE journal_entry ADD CONSTRAINT journal_entry_entry_type_check
CHECK (entry_type IN ('PAYMENT', 'CANCEL', 'PARTIAL_REFUND', 'SETTLEMENT', 'PAYOUT', 'NEGATIVE_SETTLEMENT_DEDUCTION'));