package com.campusfind.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {
            "/",
            "/login",
            "/register",
            "/post",
            "/items/**",
            "/matches",
            "/matches/**",
            "/chat/**",
            "/notifications",
            "/profile",
            "/leaderboard",
            "/map",
            "/admin"
    })
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
