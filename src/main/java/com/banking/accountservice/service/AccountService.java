package com.banking.accountservice.service;

import com.banking.accountservice.dto.CreateAccountRequest;
import com.banking.accountservice.entity.Account;
import com.banking.accountservice.entity.AccountStatus;
import com.banking.accountservice.entity.AccountType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor

public class AccountService  {
    private final AccountRepository accountRepository;
    private static SecureRandom secureRandom = new SecureRandom();

    public AccountResponse createAccount(CreateAccountRequest createAccountRequest request) {
        log.info("Creating account for: {}", request.getEmail());

        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Account already exists for email: " + request.getEmail());
        }

        Account account = new Account();
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getInitialDeposit());
        account.setAccountNumber(generateAccountNumber());
        account.setDailyTransactionLimit(
                request.getAccountType() = AccountType.SAVINGS
                        ? new BigDecimal("100000")
                        : new BigDecimal("500000")
        );

        Account savedAccount = accountRepository.save(account);
        log.info("Account created: {}", savedAccount.getAccountNumber());

        return mapToResponse(savedAccount);
    }

    /**
     * Get account by account number
     * @param accountNumber
     * @return
     */
    public AccountRespone getAccount(String accountNumber){
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account Not Found"));

        return mapToResponse(account);
    }

    /**
     * Get balance by accountNumber
     * @param accountNumber
     * @return
     */
    public BigDecimal getBalance(String accountNumber){
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account Not Found"));

        return account.getBalance();
    }

    /**
     * Block account - Fraud Detection Service via Kafka
     * @return
     */
    public void blockAccount(String accountNumber){
        log.info("Blocking account: {}", accountNumber);
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account Not Found"));

        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
        log.info("Account Blocked: {}", accountNumber);
    }

    /**
     * Called by Transaction Service
     * @param accountNumber
     * @param amount
     */
    public void deductBalance(String accountNumber, BigDecimal amount){
        log.info("Deducting balance {} from account: {}", amount, accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account Not Found"));

        if (account.getStatus() != AccountStatus.ACTIVE){
            throw new RuntimeException("Account is not active"+accountNumber);
        }
        if (account.getBalance().compareTo(amount) < 0){
            throw new RuntimeException("Insufficient funds for account"+accountNumber);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        log.info("Balance updated. New Balance: {}", account.getBalance());
    }

    /**
     * Credit Balance
     * Called by Transaction-Service via Kafka
     * @param accountNumber
     * @param amount
     */
    public void creditBalance(String accountNumber, BigDecimal amount){
        log.info("Crediting {} to account: {}", amount, accountNumber);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account Not Found"));

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        log.info("Balance Credited. New Balance: {}", account.getBalance());
    }

    

    //Generate unique 12 digit account number
    private String generateAccountNumber(){

        String accountNumber;

        do{
            long number = secureRandom.nextLong(1_000_000_000_000L);

            accountNumber = String.format("%012d", number);

        }while (accountRepository.existsByAccountNumber());

        return accountNumber;
    }


    private AccountResponse mapToResponse(Account account){
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setAccountHolderName(account.getAccountHolderName());
        response.setEmail(account.getEmail());
        response.setPhone(account.getPhone());
        response.setAccountType(account.getAccountType());
        response.setStatus(account.getStatus());
        response.setDailyTransactionLimit(account.getDailyTransactionLimit());
        response.setCreatedAt(account.getCreatedAt());

        return response;
    }

}
