package example.catalog.product;

import java.math.BigDecimal;

public record ProductView(Long id, String name, BigDecimal price) {
}
