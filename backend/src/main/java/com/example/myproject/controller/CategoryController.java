package com.example.myproject.controller;

import com.example.myproject.model.RecyclableCategory;
import com.example.myproject.repository.CategoryRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryRepository categoryRepository;


    public CategoryController(
            CategoryRepository categoryRepository) {

        this.categoryRepository = categoryRepository;
    }


    // ==========================================
    // GET ALL CATEGORIES
    // ==========================================

    @GetMapping
    public ResponseEntity<List<RecyclableCategory>>
    getAllCategories() {

        return ResponseEntity.ok(
                categoryRepository.findAll()
        );
    }


    // ==========================================
    // GET ACTIVE CATEGORIES
    // ==========================================

    @GetMapping("/active")
    public ResponseEntity<List<RecyclableCategory>> getActiveCategories() {

        return ResponseEntity.ok(
                categoryRepository.findByStatusIgnoreCase("Active")
        );
    }


    // ==========================================
    // ADD CATEGORY
    // ==========================================

    @PostMapping
    public ResponseEntity<?> addCategory(
            @RequestBody RecyclableCategory category) {

        String categoryName =
                category.getCategoryName()
                        .trim();

        // Empty name
        if (categoryName.isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            createMessage(
                                    "Category name cannot be empty."
                            )
                    );
        }

        // ==========================================
        // DUPLICATE CHECK
        // ==========================================

        if (
                categoryRepository
                        .existsByCategoryNameIgnoreCase(categoryName)
        ) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(
                            createMessage(
                                    "This category name already exists. Please enter a different category name."
                            )
                    );
        }

        category.setCategoryName(categoryName);

        RecyclableCategory savedCategory =
                categoryRepository.save(category);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedCategory);
    }


    // ==========================================
    // UPDATE CATEGORY
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Integer id,
            @RequestBody RecyclableCategory category) {

        // Find existing category
        RecyclableCategory existingCategory =
                categoryRepository
                        .findById(id)
                        .orElse(null);

        if (existingCategory == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(
                            createMessage(
                                    "Category not found."
                            )
                    );
        }

        String newCategoryName =
                category.getCategoryName()
                        .trim();

        if (newCategoryName.isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            createMessage(
                                    "Category name cannot be empty."
                            )
                    );
        }

        // ==========================================
        // DUPLICATE CHECK FOR UPDATE
        // ==========================================

        RecyclableCategory categoryWithSameName =
                categoryRepository
                        .findByCategoryNameIgnoreCase(
                                newCategoryName
                        )
                        .orElse(null);

        if (
                categoryWithSameName != null
                        &&
                        !categoryWithSameName
                                .getCategoryId()
                                .equals(id)
        ) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(
                            createMessage(
                                    "This category name already exists. Please enter a different category name."
                            )
                    );
        }

        // ==========================================
        // UPDATE FIELDS
        // ==========================================

        existingCategory.setCategoryName(newCategoryName);

        existingCategory.setRatePerKg(category.getRatePerKg());

        // ADDED: description update
        existingCategory.setDescription(category.getDescription());

        // ADDED: status update (Enable / Disable)
        if (category.getStatus() != null
                && !category.getStatus().trim().isEmpty()) {

            existingCategory.setStatus(category.getStatus().trim());
        }

        RecyclableCategory updatedCategory =
                categoryRepository.save(existingCategory);

        return ResponseEntity.ok(updatedCategory);
    }


    // ==========================================
    // MESSAGE
    // ==========================================

    private Map<String, String> createMessage(
            String message) {

        Map<String, String> response =
                new HashMap<>();

        response.put("message", message);

        return response;
    }
}