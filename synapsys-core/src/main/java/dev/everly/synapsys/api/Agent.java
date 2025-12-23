package dev.everly.synapsys.api;

public interface Agent<Request, Response> {
	Response execute(Request request);
}
