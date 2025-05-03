package ru.yuzhakov.app_wallet.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Data
@Table(name = "wallets")
@AllArgsConstructor
@NoArgsConstructor
public class Wallet {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID uuid;

    @Column(name = "balance")
    private Long balance;
}
