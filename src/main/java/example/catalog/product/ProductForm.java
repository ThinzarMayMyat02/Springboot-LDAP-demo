package example.catalog.product;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record ProductForm(
        @NotBlank @Size(max = 120) String name,
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal price) {
}
