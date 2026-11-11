package com.example.myproject.repository;
import com.example.myproject.model.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;



    public interface TransactionItemRepository extends JpaRepository<TransactionItem, Integer> {
        List<TransactionItem> findByTransactionId(Integer transactionId);
    }

