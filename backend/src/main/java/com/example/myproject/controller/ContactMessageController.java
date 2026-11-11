package com.example.myproject.controller;

import com.example.myproject.model.ContactMessage;
import com.example.myproject.repository.ContactMessageRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ContactMessageController {

    @Autowired
    private ContactMessageRepository contactMessageRepository;


    // CUSTOMER: Submit Contact Us message
    @PostMapping("/api/contact")
    public ResponseEntity<?> submitMessage(
            @RequestBody ContactMessage request) {

        if (request.getFullName() == null ||
            request.getFullName().trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Full name is required."));
        }

        if (request.getEmail() == null ||
            request.getEmail().trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email address is required."));
        }

        if (!request.getEmail().matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Please enter a valid email."));
        }

        if (request.getSubject() == null ||
            request.getSubject().trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Subject is required."));
        }

        if (request.getMessage() == null ||
            request.getMessage().trim().isEmpty()) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Message is required."));
        }

        ContactMessage contactMessage = new ContactMessage();

        contactMessage.setFullName(request.getFullName().trim());
        contactMessage.setEmail(request.getEmail().trim());
        contactMessage.setSubject(request.getSubject().trim());
        contactMessage.setMessage(request.getMessage().trim());

        // Every new inquiry starts as Unread
        contactMessage.setStatus("Unread");

        ContactMessage savedMessage =
                contactMessageRepository.save(contactMessage);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedMessage);
    }


    // ADMIN: Get all inquiries
    @GetMapping("/api/admin/contact-messages")
    public List<ContactMessage> getAllMessages() {

        return contactMessageRepository
                .findAllByOrderBySubmittedAtDesc();
    }


    // ADMIN: Get one inquiry
    @GetMapping("/api/admin/contact-messages/{id}")
    public ResponseEntity<ContactMessage> getMessageById(
            @PathVariable Integer id) {

        return contactMessageRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    // ADMIN: Mark inquiry as Read
    @PutMapping("/api/admin/contact-messages/{id}/read")
    public ResponseEntity<ContactMessage> markAsRead(
            @PathVariable Integer id) {

        return contactMessageRepository.findById(id)
                .map(contactMessage -> {

                    contactMessage.setStatus("Read");

                    ContactMessage updated =
                            contactMessageRepository.save(contactMessage);

                    return ResponseEntity.ok(updated);

                })
                .orElse(ResponseEntity.notFound().build());
    }


    // ADMIN: Delete inquiry
    @DeleteMapping("/api/admin/contact-messages/{id}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Integer id) {

        if (!contactMessageRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        contactMessageRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}