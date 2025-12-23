package dev.everly.synapsys.api;

public interface Validator<INPUT_TO_VALIDATE, VALIDATION_RESULT_TYPE> {
	VALIDATION_RESULT_TYPE validate(INPUT_TO_VALIDATE inputToValidate);
}
