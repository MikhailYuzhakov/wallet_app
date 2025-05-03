package ru.yuzhakov.app_wallet_reactive.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("public.wallets")
public class Wallet {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    private UUID uuid;
    private Long balance;
}
