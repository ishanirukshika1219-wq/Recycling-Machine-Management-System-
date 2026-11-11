package com.example.myproject.repository;

import com.example.myproject.model.RecyclableCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository
        extends JpaRepository<RecyclableCategory, Integer> {

    boolean existsByCategoryNameIgnoreCase(String categoryName);

    Optional<RecyclableCategory>
    findByCategoryNameIgnoreCase(String categoryName);

    List<RecyclableCategory> findByStatusIgnoreCase(String status);
}