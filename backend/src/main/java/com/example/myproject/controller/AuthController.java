package com.example.myproject.controller;

import com.example.myproject.model.Customer;
import com.example.myproject.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private CustomerRepository customerRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 1. Find customer by email using repository method
        Optional<Customer> customerOpt = customerRepository.findByEmail(request.getEmail());

        // 2. Validate existence and match plain password
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            if (customer.getPassword().equals(request.getPassword())) {
                return ResponseEntity.ok(customer); // Returns 200 OK with Customer JSON
            }
        }

        // 3. Return 401 Unauthorized if authentication fails
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Invalid email or password");
    }
}