package dev.everly.synapsys.api;
import java.util.Objects;
public class MessageModel {
    private final String role;
    private final String content;
    public MessageModel(String role, String content) {
        this.role = Objects.requireNonNull(role, "Role cannot be null");
        this.content = Objects.requireNonNull(content, "Content cannot be null");
    }
    public String getRole() { return role; }
    public String getContent() { return content; }
    @Override
    public String toString() {
        return "MessageModel{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageModel that = (MessageModel) o;
        return Objects.equals(role, that.role) &&
                Objects.equals(content, that.content);
    }
    @Override
    public int hashCode() {
        return Objects.hash(role, content);
    }
}