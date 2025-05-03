package ru.yuzhakov.app_wallet_reactive;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.jayway.jsonpath.InvalidJsonException;
import io.vertx.core.json.DecodeException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yuzhakov.app_wallet_reactive.domain.Wallet;
import ru.yuzhakov.app_wallet_reactive.dto.ErrorResponse;
import ru.yuzhakov.app_wallet_reactive.dto.OperationType;
import ru.yuzhakov.app_wallet_reactive.dto.WalletBalanceResponse;
import ru.yuzhakov.app_wallet_reactive.dto.WalletOperation;
import ru.yuzhakov.app_wallet_reactive.exceptions.InsufficientFundsException;
import ru.yuzhakov.app_wallet_reactive.exceptions.WalletNotFoundException;
import ru.yuzhakov.app_wallet_reactive.service.WalletService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureWebTestClient
class AppWalletReactiveApplicationTests {
    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private WalletService walletService;

    @Test
    void processWalletOperationTest_ShouldReturnOk() {
        UUID walletId = UUID.randomUUID();
        Long amount = 1L;
        WalletOperation operation = new WalletOperation(walletId, OperationType.DEPOSIT, amount);
        Wallet wallet = new Wallet(walletId, 475L);

        Wallet walletAfterDeposit = new Wallet(walletId, 476L);

        when(walletService.executeOperation(
                walletId,
                amount,
                OperationType.DEPOSIT))
            .thenReturn(Mono.just(walletAfterDeposit));

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(operation)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WalletBalanceResponse.class)
                .value(response -> {
                    assertEquals(walletId, response.getUuid());
                    assertEquals(476L, response.getBalance());
                });

        verify(walletService).executeOperation(walletId, amount, OperationType.DEPOSIT);
    }

    @Test
    void getWalletBalance_ShouldReturnOk() {
        UUID walletUUID = UUID.randomUUID();
        Long balance = 475L;

        when(walletService.getWalletBalance(walletUUID))
                .thenReturn(Mono.just(balance));

        webTestClient.get()
                .uri("/api/v1/wallets/" + walletUUID)
                .exchange()
                .expectStatus().isOk()
                .expectBody(WalletBalanceResponse.class)
                .value(response -> {
                    assertEquals(walletUUID, response.getUuid());
                    assertEquals(balance, response.getBalance());
                });

        verify(walletService).getWalletBalance(walletUUID);
    }

    @Test
    void insufficientFundsWalletOperation_ShouldReturnBadRequest() {
        UUID walletUuid = UUID.randomUUID();
        WalletOperation walletOperation = new WalletOperation(walletUuid, OperationType.WITHDRAW, 476L);
        ErrorResponse errorResponse = new ErrorResponse("WALLET_ERROR", "Insufficient funds in the account");

        when(walletService.executeOperation(
                walletOperation.getWalletUUID(),
                walletOperation.getAmount(),
                walletOperation.getOperationType()
        )).thenReturn(Mono.error(new InsufficientFundsException("Insufficient funds in the account")));

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(walletOperation)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(errorResponse.getCode(), response.getCode());
                    assertEquals(errorResponse.getMessage(), response.getMessage());
                });

        verify(walletService).executeOperation(walletOperation.getWalletUUID(), walletOperation.getAmount(), walletOperation.getOperationType());
    }

    @Test
    void walletNotFoundWalletOperation_ShouldReturnBadRequest() {
        WalletOperation walletOperation = new WalletOperation(UUID.randomUUID(), OperationType.WITHDRAW, 1L);
        ErrorResponse errorResponse = new ErrorResponse("WALLET_ERROR", "Wallet not found");

        when(walletService.executeOperation(
                walletOperation.getWalletUUID(),
                walletOperation.getAmount(),
                walletOperation.getOperationType()
        )).thenReturn(Mono.error(new WalletNotFoundException("Wallet not found")));

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(walletOperation)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(errorResponse.getCode(), response.getCode());
                    assertEquals(errorResponse.getMessage(), response.getMessage());
                });

        verify(walletService).executeOperation(walletOperation.getWalletUUID(), walletOperation.getAmount(), walletOperation.getOperationType());
    }

    @Test
    void notCorrectOperationType_ShouldReturnBadRequest() {
        String invalidJson = """
        {
            "walletUUID": "d0242b5f-96c9-4806-a924-61accf69de55",
            "operationType": "TRANSFER",
            "amount": 1
        }
        """;

        ErrorResponse expectedResponse = new ErrorResponse(
                "INVALID_REQUEST",
                "operationType must be either 'DEPOSIT' or 'WITHDRAW'"
        );

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(expectedResponse.getCode(), response.getCode());
                    assertEquals(expectedResponse.getMessage(), response.getMessage());
                });

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void amountNotNumber_ShouldReturnBadRequest() {
        String invalidJson = """
        {
            "walletUUID": "d0242b5f-96c9-4806-a924-61accf69de55",
            "operationType": "WITHDRAW",
            "amount": "not a number"
        }
        """;

        ErrorResponse expectedResponse = new ErrorResponse(
                "INVALID_REQUEST",
                "amount must be a number"
        );

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(expectedResponse.getCode(), response.getCode());
                    assertEquals(expectedResponse.getMessage(), response.getMessage());
                });

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void uuidNotValid_ShouldReturnBadRequest() {
        String invalidJson = """
        {
            "walletUUID": "d0242b5f-96c9-4806a924-61accf69de55",
            "operationType": "WITHDRAW",
            "amount": "1"
        }
        """;

        ErrorResponse expectedResponse = new ErrorResponse(
                "INVALID_REQUEST",
                "walletUUID must be a valid UUID format"
        );

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(expectedResponse.getCode(), response.getCode());
                    assertEquals(expectedResponse.getMessage(), response.getMessage());
                });

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void amountNotPositive_ShouldReturnBadRequest() {
        String invalidJson = """
        {
            "walletUUID": "d0242b5f-96c9-4806-a924-61accf69de55",
            "operationType": "WITHDRAW",
            "amount": "-1"
        }
        """;

        ErrorResponse expectedResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "amount: amount must be positive");

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(expectedResponse.getCode(), response.getCode());
                    assertEquals(expectedResponse.getMessage(), response.getMessage());
                });

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void invalidJson_ShouldReturnBadRequest() {
        String invalidJson = """
        {
            xas
        }
        """;

        ErrorResponse expectedResponse = new ErrorResponse(
                "INVALID_REQUEST",
                "Invalid JSON format");

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(expectedResponse.getCode(), response.getCode());
                    assertEquals(expectedResponse.getMessage(), response.getMessage());
                });

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void amountNotFound_ShouldReturnBadRequest() {
        String invalidJson = """
        {
            "walletUUID": "d0242b5f-96c9-4806-a924-61accf69de55",
            "operationType": "WITHDRAW"
        }
        """;

        ErrorResponse expectedResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "amount: amount is required");

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(expectedResponse.getCode(), response.getCode());
                    assertEquals(expectedResponse.getMessage(), response.getMessage());
                });

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void walletUuidNotFound_ShouldReturnBadRequest() {
        String invalidJson = """
        {
            "operationType": "WITHDRAW",
            "amount": 1
        }
        """;

        ErrorResponse expectedResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "walletUUID: walletUUID is required");

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(expectedResponse.getCode(), response.getCode());
                    assertEquals(expectedResponse.getMessage(), response.getMessage());
                });

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void operationTypeNotFound_ShouldReturnBadRequest() {
        String invalidJson = """
        {
            "walletUUID": "d0242b5f-96c9-4806-a924-61accf69de55",
            "amount": 1
        }
        """;

        ErrorResponse expectedResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "operationType: operationType is required");

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(expectedResponse.getCode(), response.getCode());
                    assertEquals(expectedResponse.getMessage(), response.getMessage());
                });

        verify(walletService, never()).executeOperation(any(), any(), any());
    }
}