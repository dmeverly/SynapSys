package dev.everly.synapsys.api;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class LlmCallOptions {
    private final boolean jsonMode;
    private final Double temperature;
    private final Double topP;
    private final Integer maxTokens;
    private final List<String> stopSequences;
    private final Long seed;
    private final Boolean stream;
    private final Map<String, Object> customOptions;
    private LlmCallOptions(Builder builder) {
        this.jsonMode = builder.jsonMode;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.maxTokens = builder.maxTokens;
        this.stopSequences = builder.stopSequences != null ? List.copyOf(builder.stopSequences) : Collections.emptyList();
        this.seed = builder.seed;
        this.stream = builder.stream;
        this.customOptions = builder.customOptions != null ? Map.copyOf(builder.customOptions) : Collections.emptyMap();
    }
    public boolean isJsonMode() {
        return jsonMode;
    }
    public Double getTemperature() {
        return temperature;
    }
    public Double getTopP() {
        return topP;
    }
    public Integer getMaxTokens() {
        return maxTokens;
    }
    public List<String> getStopSequences() {
        return stopSequences;
    }
    public Long getSeed() {
        return seed;
    }
    public Boolean getStream() {
        return stream;
    }
    public Map<String, Object> getCustomOptions() {
        return customOptions;
    }
    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        private boolean jsonMode = false;
        private Double temperature;
        private Double topP;
        private Integer maxTokens;
        private List<String> stopSequences;
        private Long seed;
        private Boolean stream;
        private Map<String, Object> customOptions;
        public Builder jsonMode(boolean jsonMode) {
            this.jsonMode = jsonMode;
            return this;
        }
        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }
        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }
        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }
        public Builder stopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
            return this;
        }
        public Builder seed(Long seed) {
            this.seed = seed;
            return this;
        }
        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }
        public Builder customOption(String key, Object value) {
            if (this.customOptions == null) {
                this.customOptions = new HashMap<>();
            }
            this.customOptions.put(key, value);
            return this;
        }
        public Builder customOptions(Map<String, Object> customOptions) {
            if (this.customOptions == null) {
                this.customOptions = new HashMap<>();
            }
            this.customOptions.putAll(customOptions);
            return this;
        }
        public LlmCallOptions build() {
            return new LlmCallOptions(this);
        }
    }
}