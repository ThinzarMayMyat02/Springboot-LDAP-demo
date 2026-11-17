package example.catalog.product;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/products")
class ProductController {
    private static final String BASE = "/products";
    private final ProductService service;

    ProductController(ProductService service) {
        this.service = service;
    }

    @ModelAttribute
    void page(Model model) {
        model.addAttribute("base", BASE);
        model.addAttribute("title", "Products");
        model.addAttribute("label", "Product");
        model.addAttribute("users", false);
    }

    @GetMapping
    String list(Model model) {
        model.addAttribute("rows", service.list());
        return "list";
    }

    @GetMapping("/{id}")
    String detail(@PathVariable long id, Model model) {
        model.addAttribute("row", service.get(id));
        return "detail";
    }

    @GetMapping("/new")
    String newForm(Model model) {
        model.addAttribute("form", new ProductForm(null, null));
        return formPage(model, null);
    }

    @PostMapping
    String create(@Valid @ModelAttribute("form") ProductForm form, BindingResult errors,
                  Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            return formPage(model, null);
        }
        service.create(form);
        return saved(flash, "Product created.");
    }

    @GetMapping("/{id}/edit")
    String editForm(@PathVariable long id, Model model) {
        var row = service.get(id);
        model.addAttribute("form", new ProductForm(row.name(), row.price()));
        return formPage(model, id);
    }

    @PostMapping("/{id}")
    String update(@PathVariable long id, @Valid @ModelAttribute("form") ProductForm form,
                  BindingResult errors, Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            return formPage(model, id);
        }
        service.update(id, form);
        return saved(flash, "Product updated.");
    }

    @GetMapping("/{id}/delete")
    String confirmDelete(@PathVariable long id, Model model) {
        model.addAttribute("row", service.get(id));
        return "delete";
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable long id, RedirectAttributes flash) {
        service.delete(id);
        return saved(flash, "Product deleted.");
    }

    private String formPage(Model model, Long id) {
        model.addAttribute("editing", id != null);
        model.addAttribute("action", id == null ? BASE : BASE + "/" + id);
        if (id != null) {
            model.addAttribute("row", service.get(id));
        }
        return "form";
    }

    private String saved(RedirectAttributes flash, String message) {
        flash.addFlashAttribute("message", message);
        return "redirect:" + BASE;
    }
}
