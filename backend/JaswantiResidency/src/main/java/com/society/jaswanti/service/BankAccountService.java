package com.society.jaswanti.service;

import com.society.jaswanti.dto.BankReconciliationDto;
import com.society.jaswanti.dto.CreateBankAccountRequest;
import com.society.jaswanti.dto.SocietyBankAccountDto;
import com.society.jaswanti.dto.UpdateBankAccountRequest;
import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.SocietyBankAccount;
import com.society.jaswanti.entity.Transaction;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.SocietyBankAccountRepository;
import com.society.jaswanti.repository.TransactionRepository;
import com.society.jaswanti.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BankAccountService {

    private final SocietyBankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final TransactionRepository transactionRepository;

    public BankAccountService(SocietyBankAccountRepository bankAccountRepository,
                              UserRepository userRepository,
                              ActivityLogService activityLogService,
                              TransactionRepository transactionRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
        this.activityLogService = activityLogService;
        this.transactionRepository = transactionRepository;
    }

    public List<SocietyBankAccountDto> getAllBankAccounts() {
        return bankAccountRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public SocietyBankAccountDto getBankAccountById(Long id) {
        return bankAccountRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found with ID: " + id));
    }

    public BankReconciliationDto reconcileBankAccount(Long id) {
        SocietyBankAccount account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found with ID: " + id));

        return computeReconciliation(account);
    }

    public List<BankReconciliationDto> reconcileAllBankAccounts() {
        return bankAccountRepository.findAll().stream()
                .map(this::computeReconciliation)
                .collect(Collectors.toList());
    }

    private BankReconciliationDto computeReconciliation(SocietyBankAccount account) {
        List<Transaction> transactions = transactionRepository.findByBankAccountId(account.getId());

        List<Transaction> ledgerTransactions = transactions.stream()
                .filter(t -> t.getStatus() != null &&
                        ("SUCCESS".equalsIgnoreCase(t.getStatus())
                                || "REVERSED".equalsIgnoreCase(t.getStatus())
                                || "REVERSAL".equalsIgnoreCase(t.getStatus())))
                .collect(Collectors.toList());

        BigDecimal totalCredits = ledgerTransactions.stream()
                .filter(t -> "CREDIT".equalsIgnoreCase(t.getEntryDirection()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalDebits = ledgerTransactions.stream()
                .filter(t -> "DEBIT".equalsIgnoreCase(t.getEntryDirection()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal openingBalance = account.getOpeningBalance() != null
                ? account.getOpeningBalance().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal calculatedBalance = openingBalance.add(totalCredits).subtract(totalDebits).setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentBalance = account.getCurrentBalance() != null
                ? account.getCurrentBalance().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discrepancy = currentBalance.subtract(calculatedBalance).setScale(2, RoundingMode.HALF_UP);
        boolean reconciled = discrepancy.compareTo(BigDecimal.ZERO) == 0;

        return new BankReconciliationDto(
                account.getId(),
                account.getAccountName(),
                account.getAccountType() != null ? account.getAccountType().name() : null,
                openingBalance,
                totalCredits,
                totalDebits,
                calculatedBalance,
                currentBalance,
                discrepancy,
                reconciled,
                ledgerTransactions.size(),
                LocalDateTime.now()
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public SocietyBankAccountDto createBankAccount(CreateBankAccountRequest request, Authentication auth) {
        User actor = resolveMasterAdmin(auth);

        BigDecimal openingBalance = request.getOpeningBalance() != null
                ? request.getOpeningBalance().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        SocietyBankAccount account = new SocietyBankAccount(
                request.getAccountName().trim(),
                request.getBankName() != null ? request.getBankName().trim() : null,
                request.getAccountNumber() != null ? request.getAccountNumber().trim() : null,
                request.getIfscCode() != null ? request.getIfscCode().trim() : null,
                request.getAccountType(),
                openingBalance
        );
        account = bankAccountRepository.save(account);

        activityLogService.logActivity(actor, "CREATE_BANK_ACCOUNT", "LEDGER",
                String.format("Created society bank account '%s' (%s) with opening balance ₹%s",
                        account.getAccountName(), account.getAccountType(), account.getOpeningBalance().toPlainString()));

        return toDto(account);
    }

    @Transactional(rollbackFor = Exception.class)
    public SocietyBankAccountDto updateBankAccount(Long id, UpdateBankAccountRequest request, Authentication auth) {
        User actor = resolveMasterAdmin(auth);

        SocietyBankAccount account = bankAccountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found with ID: " + id));

        String beforeDesc = String.format("Name=%s, Type=%s, OpeningBal=%s, CurrentBal=%s, Active=%s",
                account.getAccountName(), account.getAccountType(), account.getOpeningBalance(), account.getCurrentBalance(), account.isActive());

        if (request.getAccountName() != null && !request.getAccountName().isBlank()) {
            account.setAccountName(request.getAccountName().trim());
        }
        if (request.getBankName() != null) {
            account.setBankName(request.getBankName().trim());
        }
        if (request.getAccountNumber() != null) {
            account.setAccountNumber(request.getAccountNumber().trim());
        }
        if (request.getIfscCode() != null) {
            account.setIfscCode(request.getIfscCode().trim());
        }
        if (request.getAccountType() != null) {
            account.setAccountType(request.getAccountType());
        }
        if (request.getOpeningBalance() != null) {
            BigDecimal oldOpening = account.getOpeningBalance();
            BigDecimal newOpening = request.getOpeningBalance().setScale(2, RoundingMode.HALF_UP);
            BigDecimal diff = newOpening.subtract(oldOpening);
            account.setOpeningBalance(newOpening);
            account.setCurrentBalance(account.getCurrentBalance().add(diff).setScale(2, RoundingMode.HALF_UP));
        }
        if (request.getActive() != null) {
            account.setActive(request.getActive());
        }

        account = bankAccountRepository.save(account);

        String afterDesc = String.format("Name=%s, Type=%s, OpeningBal=%s, CurrentBal=%s, Active=%s",
                account.getAccountName(), account.getAccountType(), account.getOpeningBalance(), account.getCurrentBalance(), account.isActive());

        activityLogService.logActivity(actor, "UPDATE_BANK_ACCOUNT", "LEDGER",
                String.format("Updated bank account ID %d: [%s] -> [%s]", id, beforeDesc, afterDesc));

        return toDto(account);
    }

    private User resolveMasterAdmin(Authentication auth) {
        Authentication effectiveAuth = auth != null ? auth : org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (effectiveAuth != null && effectiveAuth.getName() != null && !effectiveAuth.getName().trim().isEmpty() && !"anonymousUser".equals(effectiveAuth.getName())) {
            User user = userRepository.findByEmail(effectiveAuth.getName())
                    .orElseThrow(() -> new AccessDeniedException("User not found: " + effectiveAuth.getName()));
            if (user.getRole() != Role.MASTER_ADMIN) {
                throw new AccessDeniedException("Only Master Admin can configure society bank accounts");
            }
            return user;
        }

        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.MASTER_ADMIN && u.isActive())
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("Master Admin authentication is required to configure bank accounts"));
    }

    public SocietyBankAccountDto toDto(SocietyBankAccount a) {
        return new SocietyBankAccountDto(
                a.getId(),
                a.getAccountName(),
                a.getBankName(),
                a.getAccountNumber(),
                a.getIfscCode(),
                a.getAccountType(),
                a.getOpeningBalance(),
                a.getCurrentBalance(),
                a.isActive(),
                a.getCreatedAt()
        );
    }
}
