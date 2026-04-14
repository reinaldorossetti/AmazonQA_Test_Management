UPDATE test_cases
SET test_case_status = 'Ready for Review'
WHERE lower(trim(test_case_status)) = 'ready';

CREATE OR REPLACE FUNCTION normalize_test_cases_status_ready_alias()
RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.test_case_status IS NOT NULL THEN
        IF lower(trim(NEW.test_case_status)) = 'ready' THEN
            NEW.test_case_status := 'Ready for Review';
        ELSIF lower(trim(NEW.test_case_status)) = 'ready for review' THEN
            NEW.test_case_status := 'Ready for Review';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_normalize_test_case_status_ready_alias ON test_cases;

CREATE TRIGGER trg_normalize_test_case_status_ready_alias
BEFORE INSERT OR UPDATE OF test_case_status ON test_cases
FOR EACH ROW
EXECUTE FUNCTION normalize_test_cases_status_ready_alias();
