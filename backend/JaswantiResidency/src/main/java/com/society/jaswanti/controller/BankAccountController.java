package com.society.jaswanti.controller;

import com.society.jaswanti.audit.AuditedAction;
import com.society.jaswanti.audit.AuditTargetResolver;

import com.society.jaswanti.dto.BankReconciliationDto;
import com.society.jaswanti.dto.CreateBankAccountRequest;
import com.society.jaswanti.dto.SocietyBankAccountDto;
import com.society.jaswanti.dto.UpdateBankAccountRequest;
import com.society.jaswanti.service.BankAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank-accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT')")
    public ResponseEntity<List<SocietyBankAccountDto>> getAllBankAccounts() {
        return ResponseEntity.ok(bankAccountService.getAllBankAccounts());
    }

    @GetMapping("/reconcile")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT')")
    public ResponseEntity<List<BankReconciliationDto>> reconcileAllBankAccounts() {
        return ResponseEntity.ok(bankAccountService.reconcileAllBankAccounts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT')")
    public ResponseEntity<SocietyBankAccountDto> getBankAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(bankAccountService.getBankAccountById(id));
    }

    @GetMapping("/{id}/reconcile")
    @PreAuthorize("hasAnyRole('ADMIN', 'MASTER_ADMIN', 'OFFICE_ASSISTANT')")
    public ResponseEntity<BankReconciliationDto> reconcileBankAccount(@PathVariable Long id) {
        return ResponseEntity.ok(bankAccountService.reconcileBankAccount(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    @AuditedAction(action = "CREATE_BANK_ACCOUNT", module = "LEDGER", summary = "Created a bank account", target = AuditTargetResolver.BANK_ACCOUNT)
    public ResponseEntity<SocietyBankAccountDto> createBankAccount(@Valid @RequestBody CreateBankAccountRequest request,
                                                                   Authentication auth) {
        return ResponseEntity.ok(bankAccountService.createBankAccount(request, auth));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MASTER_ADMIN')")
    @AuditedAction(action = "UPDATE_BANK_ACCOUNT", module = "LEDGER", summary = "Updated a bank account", target = AuditTargetResolver.BANK_ACCOUNT)
    public ResponseEntity<SocietyBankAccountDto> updateBankAccount(@PathVariable Long id,
                                                                   @Valid @RequestBody UpdateBankAccountRequest request,
                                                                   Authentication auth) {
        return ResponseEntity.ok(bankAccountService.updateBankAccount(id, request, auth));
    }
}
