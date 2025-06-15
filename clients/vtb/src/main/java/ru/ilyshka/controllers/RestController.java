package ru.ilyshka.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.ilyshka.dto.AccessCodeRequest;
import ru.ilyshka.dto.SmsCodeRequest;
import ru.ilyshka.servies.VtbSeleniumClient;
import ru.ilyshka.servies.VtbService;

@RequiredArgsConstructor
@org.springframework.web.bind.annotation.RestController
public class RestController {
    private final VtbSeleniumClient client;
    private final VtbService service;

    @PostMapping("/api/sms")
    public void smsCode(@RequestBody SmsCodeRequest smsCodeRequest) {
        client.writeSmsCode(smsCodeRequest.code());
    }

    @PostMapping("/api/code")
    public void accessCode(@RequestBody AccessCodeRequest smsCodeRequest) {
        client.writeAccessCode(smsCodeRequest.code());
    }

    @PostMapping("/api/action")
    public void action() {
        service.startActions();
    }
}
