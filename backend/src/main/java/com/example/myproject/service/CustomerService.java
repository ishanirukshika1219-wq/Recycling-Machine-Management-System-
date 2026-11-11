package com.example.myproject.service;

import com.example.myproject.model.Customer;
import com.example.myproject.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service

public class CustomerService {

        private final CustomerRepository customerRepository;

        public CustomerService(CustomerRepository customerRepository) {
            this.customerRepository = customerRepository;
        }

        public Customer registerCustomer(Customer customer) {

            // Check whether email already exists
            if (customerRepository.existsByEmail(customer.getEmail())) {
                throw new RuntimeException("Email already registered");
            }

            // Set registration date
            customer.setRegisteredDate(LocalDateTime.now());

            // Save customer to database
            return customerRepository.save(customer);
        }

}
