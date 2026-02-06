package dev.everly.synapsys.authentication;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Component
public class NonceCache {

	private final Cache<String, Boolean> cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(5))
			.maximumSize(200_000).build();

	public boolean markIfNew(String sender, String nonce) {
		String key = sender + ":" + nonce;
		return cache.asMap().putIfAbsent(key, Boolean.TRUE) == null;
	}
}
