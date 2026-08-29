package com.banking.accountservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    boolean existsByEmail(String email);
    boolean existByAccountNumber(String accountNumber);
    Optional<Account> findByAccountNumber(String accountNumber);
}
