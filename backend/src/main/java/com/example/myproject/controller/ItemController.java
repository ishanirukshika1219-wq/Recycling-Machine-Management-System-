package com.example.myproject.controller;

import com.example.myproject.model.Item;
import com.example.myproject.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173") // <-- CRITICAL: Allows React to send data
@RequestMapping("/api/items")
public class ItemController {

        @Autowired
        private ItemRepository itemRepository;

        @GetMapping
        public List<Item> getAllItems() {
                return itemRepository.findAll();
        }

        @PostMapping
        public Item createItem(@RequestBody Item item) {
                return itemRepository.save(item);
        }
}