package ru.yuzhakov.app_wallet_reactive.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yuzhakov.app_wallet_reactive.dto.WalletBalanceResponse;
import ru.yuzhakov.app_wallet_reactive.dto.WalletOperation;
import ru.yuzhakov.app_wallet_reactive.service.WalletService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RestWalletController {
    private final WalletService service;

    @PostMapping("/wallet")
    public Mono<ResponseEntity<WalletBalanceResponse>> processWalletOperation(@Valid @RequestBody Mono<WalletOperation> request) {
        return request
                .flatMap(req -> service.executeOperation(
                            req.getWalletUUID(),
                            req.getAmount(),
                            req.getOperationType()
                    ))
                .map(wallet -> ResponseEntity.ok(new WalletBalanceResponse(wallet.getUuid(), wallet.getBalance())));
    }

    @GetMapping("/wallets/{wallet_uuid}")
    public Mono<ResponseEntity<WalletBalanceResponse>> getWalletBalance(@PathVariable("wallet_uuid") UUID uuid) {
        return service.getWalletBalance(uuid)
                .map(balance -> ResponseEntity.ok(new WalletBalanceResponse(uuid, balance)));
    }
}
