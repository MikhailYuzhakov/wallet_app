package ru.yuzhakov.app_wallet_reactive.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yuzhakov.app_wallet_reactive.domain.Wallet;
import ru.yuzhakov.app_wallet_reactive.dto.OperationType;
import ru.yuzhakov.app_wallet_reactive.exceptions.InsufficientFundsException;
import ru.yuzhakov.app_wallet_reactive.exceptions.WalletNotFoundException;
import ru.yuzhakov.app_wallet_reactive.repository.WalletRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository repository;

    public Mono<Wallet> executeOperation(UUID uuid, Long amount, OperationType operationType) {
        return repository.findById(uuid)
                .switchIfEmpty(Mono.error(new WalletNotFoundException("Wallet not found")))
                .flatMap(wallet -> {
                    if (operationType == OperationType.DEPOSIT) {
                        return deposit(wallet, amount);
                    } else {
                        return withdraw(wallet, amount);
                    }
                });
    }

    public Mono<Long> getWalletBalance(UUID uuid) {
        return repository.findById(uuid)
                .switchIfEmpty(Mono.error(new WalletNotFoundException("Wallet not found")))
                .map(Wallet::getBalance);
    }

    private Mono<Wallet> deposit(Wallet wallet, Long amount) {
        wallet.setBalance(wallet.getBalance() + amount);
        return repository.save(wallet);
    }

    private Mono<Wallet> withdraw(Wallet wallet, Long amount) {
        if (wallet.getBalance() < amount) {
            return Mono.error(new InsufficientFundsException("Insufficient funds in the account"));
        }
        wallet.setBalance(wallet.getBalance() - amount);
        return repository.save(wallet);
    }
}
