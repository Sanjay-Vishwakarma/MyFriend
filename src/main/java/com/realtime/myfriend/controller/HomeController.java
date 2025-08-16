package com.realtime.myfriend.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/home")
public class HomeController {


    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user")
    public  String testUser() {
        return "I m user";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public  String testAdmin() {
        return "I m admin";
    }


    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("I am user");
    }



}
