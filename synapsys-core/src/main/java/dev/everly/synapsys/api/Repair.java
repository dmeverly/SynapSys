package dev.everly.synapsys.api;

public interface Repair<INPUT_TO_REPAIR, VALIDATION_RESULT_TYPE, REPAIRED_OUTPUT> {
	REPAIRED_OUTPUT repair(INPUT_TO_REPAIR originalInput, VALIDATION_RESULT_TYPE validationResult);
}
