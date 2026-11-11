package com.example.myproject.repository;

import com.example.myproject.model.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContactMessageRepository
        extends JpaRepository<ContactMessage, Integer> {

    List<ContactMessage> findAllByOrderBySubmittedAtDesc();
}