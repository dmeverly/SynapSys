package dev.everly.synapsys.providers;

public abstract class ProviderStrategy {
    private final String provider;
    private final String model;
    private final String api_key;
    private final String queryString;

    public ProviderStrategy(String provider, String model, String api_key, String queryString) {
        this.provider = provider;
        this.model = model;
        this.api_key = api_key;
        this.queryString = queryString;
    }

    protected String getProvider(){
        return this.provider;
    }

    protected String getModel(){
        return this.model;
    }

    protected String getQuery(){
        return this.queryString;
    }

    protected String getApiKey(){
        return this.api_key;
    }

    public abstract String generate();
}
