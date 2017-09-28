CREATE UNIQUE INDEX relation_classic_unique ON relation (project_id, group_from_id, group_to_id, form_id) WHERE kind = 0;
CREATE UNIQUE INDEX relation_survey_unique ON relation (project_id, group_from_id, form_id) WHERE kind = 1;
