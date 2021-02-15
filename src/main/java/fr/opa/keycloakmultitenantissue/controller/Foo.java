package fr.opa.keycloakmultitenantissue.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/foo")
public class Foo {

    @GetMapping
    public String getFoo() {
        return "Foo";
    }
}
