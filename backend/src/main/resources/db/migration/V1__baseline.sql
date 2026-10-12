-- NutriCoach foundation schema.
-- No production nutrition data are bundled by this migration.
-- IDs are supplied by the application so imports and offline workflows can remain reproducible.

CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(320),
    password_hash VARCHAR(255),
    display_name VARCHAR(160) NOT NULL,
    locale VARCHAR(35) NOT NULL DEFAULT 'en-IN',
    timezone VARCHAR(80) NOT NULL DEFAULT 'Asia/Kolkata',
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT app_user_email_not_blank CHECK (email IS NULL OR btrim(email) <> ''),
    CONSTRAINT app_user_status_valid CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED'))
);

CREATE UNIQUE INDEX app_user_email_unique
    ON app_user (lower(email))
    WHERE email IS NOT NULL AND status <> 'DELETED';

CREATE TABLE user_profile (
    user_id UUID PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    birth_date DATE,
    height_cm NUMERIC(8, 3),
    weight_kg NUMERIC(8, 3),
    activity_level VARCHAR(40),
    goal_code VARCHAR(40),
    dietary_pattern VARCHAR(60),
    preferred_unit_system VARCHAR(20) NOT NULL DEFAULT 'METRIC',
    profile_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_profile_height_positive CHECK (height_cm IS NULL OR height_cm > 0),
    CONSTRAINT user_profile_weight_positive CHECK (weight_kg IS NULL OR weight_kg > 0),
    CONSTRAINT user_profile_unit_valid CHECK (preferred_unit_system IN ('METRIC', 'IMPERIAL'))
);

CREATE TABLE dietary_constraint (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    constraint_type VARCHAR(40) NOT NULL,
    subject_code VARCHAR(160) NOT NULL,
    severity VARCHAR(24),
    is_hard_constraint BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT dietary_constraint_type_valid CHECK (
        constraint_type IN ('ALLERGY', 'INTOLERANCE', 'DIETARY_EXCLUSION', 'DISLIKE', 'PREFERENCE', 'OTHER')
    ),
    CONSTRAINT dietary_constraint_severity_valid CHECK (
        severity IS NULL OR severity IN ('UNKNOWN', 'MILD', 'MODERATE', 'SEVERE')
    ),
    CONSTRAINT dietary_constraint_subject_not_blank CHECK (btrim(subject_code) <> '')
);

CREATE INDEX dietary_constraint_user_idx ON dietary_constraint(user_id);

CREATE TABLE data_source (
    id UUID PRIMARY KEY,
    source_code VARCHAR(100) NOT NULL,
    display_name VARCHAR(220) NOT NULL,
    publisher VARCHAR(220),
    source_url TEXT,
    source_version VARCHAR(120) NOT NULL,
    accessed_on DATE,
    license_expression VARCHAR(160) NOT NULL,
    attribution_text TEXT,
    limitations TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT data_source_code_not_blank CHECK (btrim(source_code) <> ''),
    CONSTRAINT data_source_version_not_blank CHECK (btrim(source_version) <> ''),
    CONSTRAINT data_source_license_not_blank CHECK (btrim(license_expression) <> ''),
    CONSTRAINT data_source_code_version_unique UNIQUE (source_code, source_version)
);

CREATE TABLE data_rights_record (
    id UUID PRIMARY KEY,
    data_source_id UUID NOT NULL REFERENCES data_source(id) ON DELETE CASCADE,
    review_status VARCHAR(30) NOT NULL,
    electronic_storage_allowed BOOLEAN,
    redistribution_allowed BOOLEAN,
    modification_allowed BOOLEAN,
    commercial_use_allowed BOOLEAN,
    attribution_required BOOLEAN,
    permission_reference TEXT,
    reviewed_by VARCHAR(160),
    reviewed_on DATE,
    review_notes TEXT,
    supersedes_id UUID REFERENCES data_rights_record(id) ON DELETE RESTRICT,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT data_rights_status_valid CHECK (
        review_status IN ('NOT_REVIEWED', 'IN_REVIEW', 'APPROVED', 'RESTRICTED', 'REJECTED')
    ),
    CONSTRAINT data_rights_record_source_pair_unique UNIQUE (id, data_source_id)
);

CREATE UNIQUE INDEX data_rights_one_current_per_source_idx
    ON data_rights_record(data_source_id)
    WHERE is_current;

CREATE INDEX data_rights_source_history_idx
    ON data_rights_record(data_source_id, created_at DESC);

CREATE TABLE food (
    id UUID PRIMARY KEY,
    canonical_name VARCHAR(240) NOT NULL,
    food_kind VARCHAR(40) NOT NULL,
    created_by_user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retired_at TIMESTAMPTZ,
    CONSTRAINT food_name_not_blank CHECK (btrim(canonical_name) <> ''),
    CONSTRAINT food_kind_valid CHECK (
        food_kind IN ('GENERIC_FOOD', 'PACKAGED_FOOD', 'RESTAURANT_FOOD', 'USER_FOOD')
    )
);

CREATE INDEX food_name_idx ON food(lower(canonical_name));

CREATE TABLE food_version (
    id UUID PRIMARY KEY,
    food_id UUID NOT NULL REFERENCES food(id) ON DELETE RESTRICT,
    version_number INTEGER NOT NULL,
    display_name VARCHAR(240) NOT NULL,
    food_state VARCHAR(40) NOT NULL,
    reference_mass_g NUMERIC(18, 6) NOT NULL,
    edible_portion_fraction NUMERIC(9, 8),
    data_source_id UUID NOT NULL REFERENCES data_source(id) ON DELETE RESTRICT,
    data_rights_record_id UUID NOT NULL,
    source_record_id VARCHAR(240) NOT NULL,
    source_record_version VARCHAR(120) NOT NULL,
    source_accessed_on DATE,
    license_expression VARCHAR(160) NOT NULL,
    data_quality VARCHAR(32) NOT NULL DEFAULT 'UNASSESSED',
    limitations TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    content_hash VARCHAR(128) NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    effective_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT food_version_number_positive CHECK (version_number > 0),
    CONSTRAINT food_version_reference_mass_positive CHECK (reference_mass_g > 0),
    CONSTRAINT food_version_edible_fraction_valid CHECK (
        edible_portion_fraction IS NULL OR (edible_portion_fraction > 0 AND edible_portion_fraction <= 1)
    ),
    CONSTRAINT food_version_state_valid CHECK (
        food_state IN ('RAW', 'COOKED', 'PROCESSED', 'PREPARED', 'PACKAGED', 'UNKNOWN')
    ),
    CONSTRAINT food_version_quality_valid CHECK (
        data_quality IN ('UNASSESSED', 'VERIFIED_LABEL', 'CURATED_SOURCE', 'RECIPE_DERIVED', 'USER_ENTERED', 'ESTIMATED')
    ),
    CONSTRAINT food_version_rights_source_fk
        FOREIGN KEY (data_rights_record_id, data_source_id)
        REFERENCES data_rights_record(id, data_source_id) ON DELETE RESTRICT,
    CONSTRAINT food_version_food_number_unique UNIQUE (food_id, version_number),
    CONSTRAINT food_version_hash_unique UNIQUE (content_hash)
);

CREATE INDEX food_version_food_idx ON food_version(food_id, version_number DESC);
CREATE INDEX food_version_source_idx ON food_version(data_source_id, source_record_id);
CREATE INDEX food_version_published_idx ON food_version(is_published) WHERE is_published;

CREATE TABLE food_alias (
    id UUID PRIMARY KEY,
    food_id UUID NOT NULL REFERENCES food(id) ON DELETE CASCADE,
    alias_text VARCHAR(240) NOT NULL,
    language_tag VARCHAR(35) NOT NULL DEFAULT 'und',
    alias_kind VARCHAR(32) NOT NULL DEFAULT 'COMMON_NAME',
    normalized_text VARCHAR(240) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT food_alias_text_not_blank CHECK (btrim(alias_text) <> ''),
    CONSTRAINT food_alias_normalized_not_blank CHECK (btrim(normalized_text) <> ''),
    CONSTRAINT food_alias_kind_valid CHECK (
        alias_kind IN ('COMMON_NAME', 'REGIONAL_NAME', 'TRANSLITERATION', 'BRAND_NAME', 'MISSPELLING', 'OTHER')
    ),
    CONSTRAINT food_alias_unique UNIQUE (food_id, normalized_text, language_tag)
);

CREATE INDEX food_alias_search_idx ON food_alias(normalized_text);

CREATE TABLE nutrient_definition (
    code VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    canonical_unit VARCHAR(16) NOT NULL,
    category VARCHAR(40) NOT NULL,
    decimals_for_display SMALLINT NOT NULL DEFAULT 1,
    description TEXT,
    CONSTRAINT nutrient_definition_code_not_blank CHECK (btrim(code) <> ''),
    CONSTRAINT nutrient_definition_unit_valid CHECK (canonical_unit IN ('kcal', 'g', 'mg', 'ug')),
    CONSTRAINT nutrient_definition_decimals_valid CHECK (decimals_for_display BETWEEN 0 AND 8)
);

INSERT INTO nutrient_definition (code, display_name, canonical_unit, category, decimals_for_display) VALUES
    ('ENERGY', 'Energy', 'kcal', 'ENERGY', 0),
    ('PROTEIN', 'Protein', 'g', 'MACRONUTRIENT', 1),
    ('CARBOHYDRATE', 'Carbohydrate', 'g', 'MACRONUTRIENT', 1),
    ('FAT', 'Total fat', 'g', 'MACRONUTRIENT', 1),
    ('FIBER', 'Dietary fiber', 'g', 'MACRONUTRIENT', 1);

CREATE TABLE food_nutrient (
    food_version_id UUID NOT NULL REFERENCES food_version(id) ON DELETE CASCADE,
    nutrient_code VARCHAR(64) NOT NULL REFERENCES nutrient_definition(code) ON DELETE RESTRICT,
    observation_status VARCHAR(32) NOT NULL,
    amount NUMERIC(24, 12),
    reporting_limit NUMERIC(24, 12),
    method_note TEXT,
    PRIMARY KEY (food_version_id, nutrient_code),
    CONSTRAINT food_nutrient_status_valid CHECK (
        observation_status IN ('MEASURED', 'ESTIMATED', 'BELOW_REPORTING_LIMIT', 'MISSING', 'NOT_APPLICABLE')
    ),
    CONSTRAINT food_nutrient_amount_nonnegative CHECK (amount IS NULL OR amount >= 0),
    CONSTRAINT food_nutrient_limit_positive CHECK (reporting_limit IS NULL OR reporting_limit > 0),
    CONSTRAINT food_nutrient_value_shape CHECK (
        (observation_status IN ('MEASURED', 'ESTIMATED') AND amount IS NOT NULL AND reporting_limit IS NULL)
        OR (observation_status = 'BELOW_REPORTING_LIMIT' AND amount IS NULL AND reporting_limit IS NOT NULL)
        OR (observation_status IN ('MISSING', 'NOT_APPLICABLE') AND amount IS NULL AND reporting_limit IS NULL)
    )
);

CREATE TABLE serving_conversion (
    id UUID PRIMARY KEY,
    food_version_id UUID NOT NULL REFERENCES food_version(id) ON DELETE CASCADE,
    serving_name VARCHAR(160) NOT NULL,
    quantity NUMERIC(18, 6) NOT NULL DEFAULT 1,
    mass_g NUMERIC(18, 6) NOT NULL,
    source_kind VARCHAR(32) NOT NULL,
    confidence VARCHAR(24) NOT NULL DEFAULT 'UNKNOWN',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT serving_conversion_quantity_positive CHECK (quantity > 0),
    CONSTRAINT serving_conversion_mass_positive CHECK (mass_g > 0),
    CONSTRAINT serving_conversion_source_valid CHECK (
        source_kind IN ('LABEL', 'SOURCE_DATASET', 'USER_CALIBRATED', 'CURATED_GENERIC', 'ESTIMATED')
    ),
    CONSTRAINT serving_conversion_confidence_valid CHECK (
        confidence IN ('HIGH', 'MEDIUM', 'LOW', 'UNKNOWN')
    )
);

CREATE INDEX serving_conversion_food_idx ON serving_conversion(food_version_id);

CREATE TABLE recipe (
    id UUID PRIMARY KEY,
    owner_user_id UUID REFERENCES app_user(id) ON DELETE CASCADE,
    canonical_name VARCHAR(240) NOT NULL,
    visibility VARCHAR(24) NOT NULL DEFAULT 'PRIVATE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retired_at TIMESTAMPTZ,
    CONSTRAINT recipe_name_not_blank CHECK (btrim(canonical_name) <> ''),
    CONSTRAINT recipe_visibility_valid CHECK (visibility IN ('PRIVATE', 'INSTANCE', 'PUBLIC'))
);

CREATE INDEX recipe_owner_idx ON recipe(owner_user_id);

CREATE TABLE recipe_version (
    id UUID PRIMARY KEY,
    recipe_id UUID NOT NULL REFERENCES recipe(id) ON DELETE RESTRICT,
    version_number INTEGER NOT NULL,
    cooked_batch_mass_g NUMERIC(18, 6) NOT NULL,
    declared_servings NUMERIC(18, 6),
    notes TEXT,
    uncertainty_notes TEXT,
    calculation_contract_version VARCHAR(64) NOT NULL,
    content_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT recipe_version_number_positive CHECK (version_number > 0),
    CONSTRAINT recipe_version_batch_positive CHECK (cooked_batch_mass_g > 0),
    CONSTRAINT recipe_version_servings_positive CHECK (declared_servings IS NULL OR declared_servings > 0),
    CONSTRAINT recipe_version_recipe_number_unique UNIQUE (recipe_id, version_number),
    CONSTRAINT recipe_version_hash_unique UNIQUE (content_hash)
);

CREATE TABLE recipe_ingredient (
    id UUID PRIMARY KEY,
    recipe_version_id UUID NOT NULL REFERENCES recipe_version(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    food_version_id UUID REFERENCES food_version(id) ON DELETE RESTRICT,
    nested_recipe_version_id UUID REFERENCES recipe_version(id) ON DELETE RESTRICT,
    input_mass_g NUMERIC(18, 6) NOT NULL,
    preparation_note TEXT,
    CONSTRAINT recipe_ingredient_position_nonnegative CHECK (position >= 0),
    CONSTRAINT recipe_ingredient_mass_positive CHECK (input_mass_g > 0),
    CONSTRAINT recipe_ingredient_exactly_one_source CHECK (
        (food_version_id IS NOT NULL AND nested_recipe_version_id IS NULL)
        OR (food_version_id IS NULL AND nested_recipe_version_id IS NOT NULL)
    ),
    CONSTRAINT recipe_ingredient_position_unique UNIQUE (recipe_version_id, position)
);

CREATE INDEX recipe_ingredient_food_idx ON recipe_ingredient(food_version_id);

CREATE TABLE calculation_record (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
    calculation_type VARCHAR(40) NOT NULL,
    calculation_contract_version VARCHAR(64) NOT NULL,
    input_snapshot JSONB NOT NULL,
    result_snapshot JSONB NOT NULL,
    input_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT calculation_record_type_valid CHECK (
        calculation_type IN ('FOOD_PORTION', 'RECIPE_BATCH', 'RECIPE_PORTION', 'DAILY_TOTAL', 'RECOMMENDATION')
    )
);

CREATE INDEX calculation_record_user_time_idx ON calculation_record(user_id, created_at DESC);
CREATE INDEX calculation_record_hash_idx ON calculation_record(input_hash);

CREATE TABLE food_log (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    consumed_at TIMESTAMPTZ NOT NULL,
    meal_slot VARCHAR(32),
    food_version_id UUID REFERENCES food_version(id) ON DELETE RESTRICT,
    recipe_version_id UUID REFERENCES recipe_version(id) ON DELETE RESTRICT,
    portion_mass_g NUMERIC(18, 6) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'CONSUMED',
    calculation_record_id UUID NOT NULL REFERENCES calculation_record(id) ON DELETE RESTRICT,
    user_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT food_log_portion_positive CHECK (portion_mass_g > 0),
    CONSTRAINT food_log_status_valid CHECK (status IN ('PLANNED', 'SUGGESTED', 'CONSUMED', 'SKIPPED')),
    CONSTRAINT food_log_exactly_one_source CHECK (
        (food_version_id IS NOT NULL AND recipe_version_id IS NULL)
        OR (food_version_id IS NULL AND recipe_version_id IS NOT NULL)
    )
);

CREATE INDEX food_log_user_time_idx ON food_log(user_id, consumed_at DESC);
CREATE INDEX food_log_status_idx ON food_log(user_id, status);

CREATE TABLE availability_item (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    food_version_id UUID REFERENCES food_version(id) ON DELETE RESTRICT,
    recipe_version_id UUID REFERENCES recipe_version(id) ON DELETE RESTRICT,
    availability_kind VARCHAR(32) NOT NULL,
    available_mass_g NUMERIC(18, 6),
    available_from TIMESTAMPTZ,
    available_until TIMESTAMPTZ,
    price_amount NUMERIC(18, 2),
    price_currency CHAR(3),
    preparation_minutes INTEGER,
    storage_context JSONB NOT NULL DEFAULT '{}'::jsonb,
    confirmed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT availability_exactly_one_source CHECK (
        (food_version_id IS NOT NULL AND recipe_version_id IS NULL)
        OR (food_version_id IS NULL AND recipe_version_id IS NOT NULL)
    ),
    CONSTRAINT availability_kind_valid CHECK (
        availability_kind IN ('PANTRY', 'PREPARED', 'HOSTEL_MENU', 'CAFETERIA', 'RESTAURANT', 'PURCHASABLE')
    ),
    CONSTRAINT availability_mass_positive CHECK (available_mass_g IS NULL OR available_mass_g > 0),
    CONSTRAINT availability_price_nonnegative CHECK (price_amount IS NULL OR price_amount >= 0),
    CONSTRAINT availability_minutes_nonnegative CHECK (preparation_minutes IS NULL OR preparation_minutes >= 0),
    CONSTRAINT availability_window_valid CHECK (
        available_from IS NULL OR available_until IS NULL OR available_until > available_from
    )
);

CREATE INDEX availability_user_time_idx ON availability_item(user_id, available_until);

CREATE TABLE recommendation_record (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    ruleset_version VARCHAR(64) NOT NULL,
    target_method_version VARCHAR(64),
    scoring_weight_version VARCHAR(64) NOT NULL,
    context_snapshot JSONB NOT NULL,
    candidate_snapshot JSONB NOT NULL,
    selected_candidate_ids JSONB NOT NULL,
    explanation_snapshot JSONB NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT recommendation_status_valid CHECK (
        status IN ('FEASIBLE', 'NO_FEASIBLE_RESULT', 'ACCEPTED', 'REJECTED', 'EXPIRED')
    )
);

CREATE INDEX recommendation_user_time_idx ON recommendation_record(user_id, created_at DESC);

CREATE TABLE recommendation_feedback (
    id UUID PRIMARY KEY,
    recommendation_id UUID NOT NULL REFERENCES recommendation_record(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    feedback_code VARCHAR(48) NOT NULL,
    feedback_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT recommendation_feedback_code_valid CHECK (
        feedback_code IN (
            'ATE_THIS', 'SKIPPED', 'UNAVAILABLE', 'TOO_LARGE', 'TOO_SMALL',
            'DISLIKED', 'ESTIMATE_INCORRECT', 'OTHER'
        )
    )
);

CREATE INDEX recommendation_feedback_recommendation_idx
    ON recommendation_feedback(recommendation_id, created_at DESC);
