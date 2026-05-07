package com.claircore.billing.interfaces.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaticWebController {

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    @GetMapping("/checkout-demo")
    public String checkout(Model model) {
        model.addAttribute("stripePublicKey", stripePublicKey);
        return "checkout";
    }
}
