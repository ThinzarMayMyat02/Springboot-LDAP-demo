package example.catalog.user;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
class UserProfileController {
    private static final String BASE = "/admin/users";
    private final UserProfileService service;

    UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @ModelAttribute
    void page(Model model) {
        model.addAttribute("base", BASE);
        model.addAttribute("title", "Users");
        model.addAttribute("label", "User account");
        model.addAttribute("users", true);
    }

    @GetMapping
    String list(Model model) {
        model.addAttribute("rows", service.list());
        return "list";
    }

    @GetMapping("/{id}")
    String detail(@PathVariable String id, Model model) {
        model.addAttribute("row", service.get(id));
        return "detail";
    }

    @GetMapping("/new")
    String newForm(Model model) {
        model.addAttribute("form", new UserProfileCreateForm(null, null, null, null));
        return formPage(model, null);
    }

    @PostMapping
    String create(@Valid @ModelAttribute("form") UserProfileCreateForm form, BindingResult errors,
                  Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            return formPage(model, null);
        }
        try {
            service.create(form);
        } catch (ProfileUsernameException exception) {
            errors.reject("directory", exception.getMessage());
            return formPage(model, null);
        }
        return saved(flash, "User account created.");
    }

    @GetMapping("/{id}/edit")
    String editForm(@PathVariable String id, Model model) {
        var row = service.get(id);
        model.addAttribute("form", new UserProfileForm(row.displayName(), row.email()));
        return formPage(model, id);
    }

    @PostMapping("/{id}")
    String update(@PathVariable String id, @Valid @ModelAttribute("form") UserProfileForm form,
                  BindingResult errors, Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            return formPage(model, id);
        }
        try {
            service.update(id, form);
        } catch (ProfileUsernameException exception) {
            errors.reject("directory", exception.getMessage());
            return formPage(model, id);
        }
        return saved(flash, "User account updated.");
    }

    @GetMapping("/{id}/delete")
    String confirmDelete(@PathVariable String id, Model model) {
        model.addAttribute("row", service.get(id));
        return "delete";
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable String id, RedirectAttributes flash) {
        service.delete(id);
        return saved(flash, "User account deleted.");
    }

    private String formPage(Model model, String id) {
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
