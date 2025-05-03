package ru.yuzhakov.app_wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import jakarta.validation.constraints.*;

import java.util.UUID;

@Data
@AllArgsConstructor
public class WalletOperation {

    @NotNull(message = "walletUUID is required")
    private UUID walletUUID;

    @NotNull(message = "operationType is required")
    private OperationType operationType;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private Long amount;
}
