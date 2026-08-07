UPDATE processes.application_type_enum
SET target_call = REPLACE(target_call, 'devefdmmart', 'dev-eafdmmart')
WHERE target_call LIKE '%devefdmmart%';
