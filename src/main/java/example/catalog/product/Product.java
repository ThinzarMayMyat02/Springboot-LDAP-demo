package example.catalog.product;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    protected Product() {
    }

    Product(String name, BigDecimal price) {
        update(name, price);
    }

    void update(String name, BigDecimal price) {
        this.name = name;
        this.price = price;
    }

    ProductView toView() {
        return new ProductView(id, name, price);
    }
}
