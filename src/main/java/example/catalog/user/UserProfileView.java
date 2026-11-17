package example.catalog.user;

public record UserProfileView(String username, String displayName, String email, UserRole role) {
    public String id() {
        return username;
    }
}
