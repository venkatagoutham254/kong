package aforo.kong.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerRedirectController {

    @GetMapping("/")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui.html";
    }

    @GetMapping("/swagger")
    public String redirectToSwaggerAlternate() {
        return "redirect:/swagger-ui.html";
    }
}
