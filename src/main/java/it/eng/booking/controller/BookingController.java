package it.eng.booking.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {

    @GetMapping("/hello")
    public String hello() {
        return "Booking API funzionante!";
    }

    @GetMapping("/camere")
    public List<String> getCamere() {
        return List.of("Camera singola", "Camera doppia", "Camera familiare");
    }
}
