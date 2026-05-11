package dev.everly.synapsys.dto;

import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public abstract class Message {

	private final String provider;
    private final String model;
    private final String payload;
    private final Instant timestamp;

	@JsonCreator
	protected Message(@JsonProperty("provider") String provider, @JsonProperty("model") String model, @JsonProperty("payload") String payload) {
		this.provider = requireNonBlank(provider, "provider");
		this.model = requireNonBlank(model, "model");
		this.payload = requireNonBlank(payload, "payload");
        this.timestamp = Instant.now();
	}

	protected static String requireNonBlank(String value, String field) {
		String trimmed = Objects.requireNonNull(value, field + " must not be null").trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return trimmed;
	}

    public String getProvider(){
        return this.provider;
    }

    public String getModel(){
        return this.model;
    }

    public String getPayload(){
        return this.payload;
    }

    public String getTimeStamp(){
        return this.timestamp.toString();
    }

}