package example.catalog.user;

public record UserProfileView(String username, String displayName, String email) {
    public String id() {
        return username;
    }
}
