package org.mskcc.knowledge.controller;

import io.swagger.annotations.Api;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Controller
public class WebController {
    @RequestMapping(value = {"/"})
    public String index() {
        return "index.html";
    }

    @RequestMapping(value = {"/search"})
    public String search() {
        return "/";
    }

    @RequestMapping(value = {"/results"})
    public String results() {
        return "/";
    }

    @RequestMapping(value = {"/item"})
    public String items() {
        return "/";
    }

    @RequestMapping(value = {"/comment"})
    public String comment() {
        return "/";
    }

    @RequestMapping(value = {"/article/**"})
    public String records() {
        return "/";
    }

    @RequestMapping(value = {"/genes/**"})
    public String targets() {
        return "/";
    }

}
