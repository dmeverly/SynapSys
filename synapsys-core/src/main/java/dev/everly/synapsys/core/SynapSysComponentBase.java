package dev.everly.synapsys.core;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
public abstract class SynapSysComponentBase implements ISynapSysComponent {
    private final String id;
    private final String name;
    protected final Map<String, Object> parameters;
    public SynapSysComponentBase(String id, String name) {
        this.id = Objects.requireNonNull(id, "Component ID cannot be null.");
        this.name = Objects.requireNonNull(name, "Component Name cannot be null.");
        if (id.trim().isEmpty()) {
            throw new IllegalArgumentException("Component ID cannot be empty.");
        }
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Component Name cannot be empty.");
        }
        this.parameters = new HashMap<>();
    }
    @Override
    public String getID() {
        return id;
    }
    @Override
    public String getName() {
        return name;
    }
    public void setParameter(String key, Object value) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Parameter key cannot be null or empty.");
        }
        this.parameters.put(key, value);
    }
    public Object getParameter(String key) {
        return this.parameters.get(key);
    }
    public <T> T getParameter(String key, Class<T> type) {
        Object value = this.parameters.get(key);
        if (type != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
    public Map<String, Object> getAllParameters() {
        return Collections.unmodifiableMap(parameters);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SynapSysComponentBase that = (SynapSysComponentBase) o;
        return Objects.equals(id, that.id);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}