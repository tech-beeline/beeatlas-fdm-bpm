UPDATE processes.application_type_enum
SET target_call = '/api/v1/business-capability/public/{id}'
WHERE alias IN ('create_business_capability', 'update_business_capability')
  AND target_call IS NOT NULL
  AND target_call LIKE 'http%';