-- public.account definition

-- Drop table

-- DROP TABLE public.account;

CREATE TABLE public.account (
	account_id varchar(255) NOT NULL,
	account_type varchar(255) NULL,
	"name" varchar(255) NULL,
	owner_id varchar(255) NULL,
	CONSTRAINT account_account_type_check CHECK (((account_type)::text = ANY ((ARRAY['ASSET'::character varying, 'LIABILITY'::character varying, 'REVENUE'::character varying])::text[]))),
	CONSTRAINT account_pkey PRIMARY KEY (account_id)
);


-- public.fee_policy definition

-- Drop table

-- DROP TABLE public.fee_policy;

CREATE TABLE public.fee_policy (
	effective_from date NULL,
	effective_to date NULL,
	fee_rate numeric(38, 2) NULL,
	card_company varchar(255) NULL,
	fee_policy_id varchar(255) NOT NULL,
	merchant_id varchar(255) NULL,
	CONSTRAINT fee_policy_card_company_check CHECK (((card_company)::text = ANY ((ARRAY['SHINHAN'::character varying, 'KB'::character varying, 'HYUNDAI'::character varying, 'SAMSUNG'::character varying, 'LOTTE'::character varying, 'HANA'::character varying, 'WOORI'::character varying, 'BC'::character varying])::text[]))),
	CONSTRAINT fee_policy_pkey PRIMARY KEY (fee_policy_id)
);


-- public.journal_entry definition

-- Drop table

-- DROP TABLE public.journal_entry;

CREATE TABLE public.journal_entry (
	occurred_at timestamp(6) NULL,
	description varchar(255) NULL,
	entry_id varchar(255) NOT NULL,
	entry_type varchar(255) NULL,
	reference_id varchar(255) NULL,
	CONSTRAINT journal_entry_entry_type_check CHECK (((entry_type)::text = ANY ((ARRAY['PAYMENT'::character varying, 'CANCEL'::character varying, 'PARTIAL_REFUND'::character varying, 'SETTLEMENT'::character varying, 'PAYOUT'::character varying])::text[]))),
	CONSTRAINT journal_entry_pkey PRIMARY KEY (entry_id)
);


-- public.merchant definition

-- Drop table

-- DROP TABLE public.merchant;

CREATE TABLE public.merchant (
	account_holder varchar(255) NULL,
	account_number varchar(255) NULL,
	bank_name varchar(255) NULL,
	business_number varchar(255) NOT NULL,
	merchant_id varchar(255) NOT NULL,
	"name" varchar(255) NULL,
	settlement_cycle varchar(255) NULL,
	CONSTRAINT merchant_business_number_key UNIQUE (business_number),
	CONSTRAINT merchant_pkey PRIMARY KEY (merchant_id),
	CONSTRAINT merchant_settlement_cycle_check CHECK (((settlement_cycle)::text = ANY ((ARRAY['D_PLUS_1'::character varying, 'D_PLUS_2'::character varying, 'WEEKLY'::character varying])::text[])))
);


-- public.payout definition

-- Drop table

-- DROP TABLE public.payout;

CREATE TABLE public.payout (
	amount numeric(38, 2) NULL,
	attempt_count int4 NULL,
	completed_at timestamp(6) NULL,
	account_holder varchar(255) NULL,
	account_number varchar(255) NULL,
	bank_name varchar(255) NULL,
	currency varchar(255) NULL,
	failure_reason varchar(255) NULL,
	failure_type varchar(255) NULL,
	merchant_id varchar(255) NULL,
	payout_id varchar(255) NOT NULL,
	settlement_id varchar(255) NULL,
	status varchar(255) NULL,
	CONSTRAINT payout_currency_check CHECK (((currency)::text = ANY ((ARRAY['KRW'::character varying, 'USD'::character varying, 'EUR'::character varying])::text[]))),
	CONSTRAINT payout_failure_type_check CHECK (((failure_type)::text = ANY ((ARRAY['TEMPORARY'::character varying, 'PERMANENT'::character varying])::text[]))),
	CONSTRAINT payout_pkey PRIMARY KEY (payout_id),
	CONSTRAINT payout_status_check CHECK (((status)::text = ANY ((ARRAY['REQUESTED'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[])))
);


-- public.settlement definition

-- Drop table

-- DROP TABLE public.settlement;

CREATE TABLE public.settlement (
	gross_amount numeric(38, 2) NULL,
	net_amount numeric(38, 2) NULL,
	payout_date date NULL,
	target_date date NULL,
	total_fee numeric(38, 2) NULL,
	currency varchar(255) NULL,
	merchant_id varchar(255) NULL,
	settlement_cycle varchar(255) NULL,
	settlement_id varchar(255) NOT NULL,
	status varchar(255) NULL,
	CONSTRAINT settlement_currency_check CHECK (((currency)::text = ANY ((ARRAY['KRW'::character varying, 'USD'::character varying, 'EUR'::character varying])::text[]))),
	CONSTRAINT settlement_merchant_id_target_date_key UNIQUE (merchant_id, target_date),
	CONSTRAINT settlement_pkey PRIMARY KEY (settlement_id),
	CONSTRAINT settlement_settlement_cycle_check CHECK (((settlement_cycle)::text = ANY ((ARRAY['D_PLUS_1'::character varying, 'D_PLUS_2'::character varying, 'WEEKLY'::character varying])::text[]))),
	CONSTRAINT settlement_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CALCULATED'::character varying, 'PAID'::character varying, 'FAILED'::character varying])::text[])))
);


-- public."transaction" definition

-- Drop table

-- DROP TABLE public."transaction";

CREATE TABLE public."transaction" (
	amount numeric(38, 2) NULL,
	settlement_date date NULL,
	approved_at timestamp(6) NULL,
	card_company varchar(255) NULL,
	currency varchar(255) NULL,
	external_transaction_id varchar(255) NULL,
	merchant_id varchar(255) NULL,
	transaction_id varchar(255) NOT NULL,
	transaction_type varchar(255) NULL,
	CONSTRAINT transaction_card_company_check CHECK (((card_company)::text = ANY ((ARRAY['SHINHAN'::character varying, 'KB'::character varying, 'HYUNDAI'::character varying, 'SAMSUNG'::character varying, 'LOTTE'::character varying, 'HANA'::character varying, 'WOORI'::character varying, 'BC'::character varying])::text[]))),
	CONSTRAINT transaction_currency_check CHECK (((currency)::text = ANY ((ARRAY['KRW'::character varying, 'USD'::character varying, 'EUR'::character varying])::text[]))),
	CONSTRAINT transaction_external_transaction_id_key UNIQUE (external_transaction_id),
	CONSTRAINT transaction_pkey PRIMARY KEY (transaction_id),
	CONSTRAINT transaction_transaction_type_check CHECK (((transaction_type)::text = ANY ((ARRAY['PAYMENT'::character varying, 'CANCEL'::character varying, 'PARTIAL_REFUND'::character varying])::text[])))
);


-- public.journal_line definition

-- Drop table

-- DROP TABLE public.journal_line;

CREATE TABLE public.journal_line (
	amount numeric(38, 2) NULL,
	account_id varchar(255) NULL,
	currency varchar(255) NULL,
	direction varchar(255) NULL,
	entry_id varchar(255) NULL,
	line_id varchar(255) NOT NULL,
	CONSTRAINT journal_line_currency_check CHECK (((currency)::text = ANY ((ARRAY['KRW'::character varying, 'USD'::character varying, 'EUR'::character varying])::text[]))),
	CONSTRAINT journal_line_direction_check CHECK (((direction)::text = ANY ((ARRAY['DEBIT'::character varying, 'CREDIT'::character varying])::text[]))),
	CONSTRAINT journal_line_pkey PRIMARY KEY (line_id),
	CONSTRAINT fk52dmb5sxvfobn2uqcjr9ri0ow FOREIGN KEY (entry_id) REFERENCES public.journal_entry(entry_id)
);