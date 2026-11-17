package example.catalog.user;

import jakarta.validation.constraints.*;

public record UserProfileForm(
        @NotBlank @Size(max = 120) String displayName,
        @NotBlank @Email @Size(max = 254) String email) {
}
