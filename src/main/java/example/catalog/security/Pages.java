package example.catalog.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Pages {
    @GetMapping("/")
    String home() {
        return "redirect:/products";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }
}
