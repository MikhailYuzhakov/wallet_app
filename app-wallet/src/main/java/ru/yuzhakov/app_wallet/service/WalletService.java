package ru.yuzhakov.app_wallet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yuzhakov.app_wallet.domain.Wallet;
import ru.yuzhakov.app_wallet.dto.OperationType;
import ru.yuzhakov.app_wallet.exceptions.InsufficientFundsException;
import ru.yuzhakov.app_wallet.exceptions.WalletNotFoundException;
import ru.yuzhakov.app_wallet.repository.WalletRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository repository;

    public Wallet executeOperation(UUID uuid, Long amount, OperationType operationType) {
        Wallet wallet = repository.findByUuid(uuid)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        if (operationType.equals(OperationType.DEPOSIT)) {
            return deposit(wallet, amount);
        } else {
            return withdraw(wallet, amount);
        }
    }

    private Wallet deposit(Wallet wallet, Long amount) {
        wallet.setBalance(wallet.getBalance() + amount);
        return repository.save(wallet);
    }

    private Wallet withdraw(Wallet wallet, Long amount) {
        if (wallet.getBalance() - amount < 0) {
            throw new InsufficientFundsException("Insufficient funds in the account");
        } else {
            wallet.setBalance(wallet.getBalance() - amount);
            return repository.save(wallet);
        }
    }

        public Long getWalletBalance(UUID uuid) {
            return repository.findByUuid(uuid)
                    .map(Wallet::getBalance)
                    .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
        }
}
