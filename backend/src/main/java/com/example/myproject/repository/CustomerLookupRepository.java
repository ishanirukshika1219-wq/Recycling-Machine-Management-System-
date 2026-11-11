package com.example.myproject.repository;

import com.example.myproject.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/** Used by the staff counter to find a customer by phone number. */
public interface CustomerLookupRepository extends JpaRepository<Customer, Integer> {

    List<Customer> findByPhoneIn(Collection<String> phones);
}