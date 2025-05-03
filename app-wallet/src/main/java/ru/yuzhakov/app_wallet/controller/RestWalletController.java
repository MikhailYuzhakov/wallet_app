package ru.yuzhakov.app_wallet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yuzhakov.app_wallet.domain.Wallet;
import ru.yuzhakov.app_wallet.dto.WalletBalanceResponse;
import ru.yuzhakov.app_wallet.dto.WalletOperation;
import ru.yuzhakov.app_wallet.service.WalletService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RestWalletController {
    private final WalletService service;

    @PostMapping("/wallet")
    public ResponseEntity<WalletBalanceResponse> processWalletOperation(@Valid @RequestBody WalletOperation request) {
        Wallet wallet = service.executeOperation(request.getWalletUUID(),
                                request.getAmount(),
                                request.getOperationType());
        return ResponseEntity.ok(new WalletBalanceResponse(wallet.getUuid(), wallet.getBalance()));
    }

    @GetMapping("/wallets/{wallet_uuid}")
    public ResponseEntity<WalletBalanceResponse> getWalletBalance(@PathVariable("wallet_uuid") UUID uuid) {
        Long balance = service.getWalletBalance(uuid);
        return ResponseEntity.ok(new WalletBalanceResponse(uuid, balance));
    }
}
