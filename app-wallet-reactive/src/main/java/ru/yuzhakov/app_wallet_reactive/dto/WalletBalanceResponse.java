package ru.yuzhakov.app_wallet_reactive.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class WalletBalanceResponse {
    private UUID uuid;
    private Long balance;
}
