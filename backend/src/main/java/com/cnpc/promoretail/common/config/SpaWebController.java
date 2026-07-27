package com.cnpc.promoretail.common.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaWebController {

    @GetMapping({
            "/",
            "/checkout",
            "/dashboard",
            "/operation-campaigns",
            "/import",
            "/inventory",
            "/rules",
            "/poster"
    })
    public String frontend() {
        return "forward:/index.html";
    }
}
