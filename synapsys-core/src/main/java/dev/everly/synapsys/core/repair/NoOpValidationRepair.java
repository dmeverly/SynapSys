package dev.everly.synapsys.core.repair;
import dev.everly.synapsys.api.Repair;
import dev.everly.synapsys.api.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class NoOpValidationRepair<T> implements Repair<T, ValidationResult, T> {
    private static final Logger log = LoggerFactory.getLogger(NoOpValidationRepair.class);
    @Override
    public T repair(T originalInput, ValidationResult validationResult) {
        log.warn("No-op validation repair mechanism used. Returning null, indicating no automatic repair for validation errors: {}", validationResult.getErrors());
        return null;
    }
}