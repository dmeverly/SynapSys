package dev.everly.synapsys.api;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class ValidationResult {
    private final boolean success;
    private final List<String> errors;

    private ValidationResult(boolean success, List<String> errors) {
        this.success = success;
        this.errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : Collections.emptyList();
    }

    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult fail(String error) {
        return new ValidationResult(false, Collections.singletonList(error));
    }

    public static ValidationResult fail(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "success=" + success +
                ", errors=" + errors +
                '}';
    }
}