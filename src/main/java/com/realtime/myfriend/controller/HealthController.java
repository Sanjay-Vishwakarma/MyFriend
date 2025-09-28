package com.realtime.myfriend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/actuator/ready")
    public Map<String,Object> ready() {
        return Map.of("status", "UP");
    }
}
