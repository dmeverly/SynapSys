package dev.everly.synapsys.core.repair;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.everly.synapsys.api.ParsingException;
import dev.everly.synapsys.api.Repair;
import dev.everly.synapsys.core.guards.JsonGuards;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class JsonGuardsParsingRepair implements Repair<String, ParsingException, String> {
    private static final Logger log = LoggerFactory.getLogger(JsonGuardsParsingRepair.class);
    private final ObjectMapper objectMapper;
    public JsonGuardsParsingRepair() {
        this.objectMapper = new ObjectMapper();
    }
    public JsonGuardsParsingRepair(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    @Override
    public String repair(String originalInput, ParsingException parsingException) {
        log.info("Attempting JSON parsing repair using JsonGuards for input (excerpt): '{}...' (Reason: {})",
                originalInput != null && originalInput.length() > 100 ? originalInput.substring(0, 100) : originalInput,
                parsingException.getMessage());
        String[] candidates = JsonGuards.candidates(originalInput);
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isEmpty() && isValidJson(candidate)) {
                log.debug("JsonGuards found a valid JSON candidate after repair.");
                return candidate;
            }
        }
        log.warn("JsonGuards failed to find a valid JSON candidate after repair attempts for input (excerpt): '{}...'",
                originalInput != null && originalInput.length() > 100 ? originalInput.substring(0, 100) : originalInput);
        return null;
    }
    private boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}