package fr.opa.keycloakmultitenantissue.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bar")
public class Bar {

    @GetMapping
    public String getBar() {
        return "Bar";
    }
}
