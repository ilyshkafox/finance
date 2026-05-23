package ru.ilyshka.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ilyshka.servies.VtbAuthService;
import ru.ilyshka.servies.VtbService;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ApiController {
    private final VtbAuthService authService;
    private final VtbService service;

    @PostMapping("/sync")
    public void sync() {
        service.sync();
    }

}
