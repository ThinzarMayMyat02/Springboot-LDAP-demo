package example.catalog.product;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository repository;

    ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<ProductView> list() {
        return repository.findAll(Sort.by("id")).stream().map(Product::toView).toList();
    }

    public ProductView get(long id) {
        return requireRecord(id).toView();
    }

    @Transactional
    public void create(ProductForm form) {
        repository.save(new Product(form.name(), form.price()));
    }

    @Transactional
    public void update(long id, ProductForm form) {
        requireRecord(id).update(form.name(), form.price());
    }

    @Transactional
    public void delete(long id) {
        repository.delete(requireRecord(id));
    }

    private Product requireRecord(long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
