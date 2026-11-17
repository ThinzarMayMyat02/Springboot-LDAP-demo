package example.catalog.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileCreateForm(
        @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9._-]{0,63}",
                message = "Use up to 64 lowercase letters, digits, dots, underscores, or hyphens.") String username,
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 128) String password,
        @NotNull UserRole role) {

    @Override
    public String toString() {
        return "UserProfileCreateForm[username=" + username + "]";
    }
}
