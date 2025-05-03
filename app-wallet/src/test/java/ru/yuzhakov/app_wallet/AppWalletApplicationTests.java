package ru.yuzhakov.app_wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yuzhakov.app_wallet.domain.Wallet;
import ru.yuzhakov.app_wallet.dto.ErrorResponse;
import ru.yuzhakov.app_wallet.dto.OperationType;
import ru.yuzhakov.app_wallet.dto.WalletOperation;
import ru.yuzhakov.app_wallet.exceptions.InsufficientFundsException;
import ru.yuzhakov.app_wallet.exceptions.WalletNotFoundException;
import ru.yuzhakov.app_wallet.service.WalletService;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AppWalletApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WalletService walletService;

    @Test
    void processWalletOperationTest_ShouldReturnOk() throws Exception {
        UUID walletId = UUID.randomUUID();
        Long amount = 1L;
        WalletOperation operation = new WalletOperation(walletId, OperationType.DEPOSIT, amount);
        Wallet wallet = new Wallet(walletId, 475L);

        Wallet walletAfterDeposit = new Wallet(walletId, 476L);

        when(walletService.executeOperation(
                walletId,
                amount,
                OperationType.DEPOSIT))
                .thenReturn(walletAfterDeposit);

        mockMvc.perform(post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(operation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(476));

        verify(walletService).executeOperation(walletId, amount, OperationType.DEPOSIT);
    }

    @Test
    void getWalletBalance_ShouldReturnOk() throws Exception {
        UUID walletUUID = UUID.randomUUID();
        Long balance = 475L;

        when(walletService.getWalletBalance(walletUUID))
                .thenReturn(balance);

        mockMvc.perform(
                get("/api/v1/wallets/" + walletUUID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(walletUUID.toString()))
                .andExpect(jsonPath("$.balance").value(balance));
        verify(walletService).getWalletBalance(walletUUID);
    }

    @Test
    void insufficientFundsWalletOperation_ShouldReturnBadRequest() throws Exception {
        UUID walletUuid = UUID.randomUUID();
        WalletOperation walletOperation = new WalletOperation(walletUuid, OperationType.WITHDRAW, 476L);
        ErrorResponse errorResponse = new ErrorResponse("WALLET_ERROR", "Insufficient funds in the account");

        when(walletService.executeOperation(
                walletOperation.getWalletUUID(),
                walletOperation.getAmount(),
                walletOperation.getOperationType()
        )).thenThrow(new InsufficientFundsException("Insufficient funds in the account"));

        mockMvc.perform(
                post("/api/v1/wallet")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(walletOperation)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value(errorResponse.getCode()))
                        .andExpect(jsonPath("$.message").value(errorResponse.getMessage()));

        verify(walletService).executeOperation(walletOperation.getWalletUUID(), walletOperation.getAmount(), walletOperation.getOperationType());
    }

    @Test
    void walletNotFoundWalletOperation_ShouldReturnBadRequest() throws Exception {
        WalletOperation walletOperation = new WalletOperation(UUID.randomUUID(), OperationType.WITHDRAW, 1L);
        ErrorResponse errorResponse = new ErrorResponse("WALLET_ERROR", "Wallet not found");

        when(walletService.executeOperation(
                walletOperation.getWalletUUID(),
                walletOperation.getAmount(),
                walletOperation.getOperationType()
        )).thenThrow(new WalletNotFoundException("Wallet not found"));

        mockMvc.perform(
                        post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(walletOperation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(errorResponse.getCode()))
                .andExpect(jsonPath("$.message").value(errorResponse.getMessage()));

        verify(walletService).executeOperation(walletOperation.getWalletUUID(), walletOperation.getAmount(), walletOperation.getOperationType());
    }

    @Test
    void notCorrectOperationType_ShouldReturnBadRequest() throws Exception {
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

        mockMvc.perform(
                        post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(expectedResponse.getCode()))
                .andExpect(jsonPath("$.message").value(expectedResponse.getMessage()));

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void amountNotNumber_ShouldReturnBadRequest() throws Exception {
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

        mockMvc.perform(
                        post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(expectedResponse.getCode()))
                .andExpect(jsonPath("$.message").value(expectedResponse.getMessage()));

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void uuidNotValid_ShouldReturnBadRequest() throws Exception {
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

        mockMvc.perform(
                        post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(expectedResponse.getCode()))
                .andExpect(jsonPath("$.message").value(expectedResponse.getMessage()));

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void amountNotPositive_ShouldReturnBadRequest() throws Exception {
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

        mockMvc.perform(
                        post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(expectedResponse.getCode()))
                .andExpect(jsonPath("$.message").value(expectedResponse.getMessage()));

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void invalidJson_ShouldReturnBadRequest() throws Exception {
        String invalidJson = """
        {
            xas
        }
        """;

        ErrorResponse expectedResponse = new ErrorResponse(
                "INVALID_REQUEST",
                "Invalid JSON format");

        mockMvc.perform(
                        post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(expectedResponse.getCode()))
                .andExpect(jsonPath("$.message").value(expectedResponse.getMessage()));

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void amountNotFound_ShouldReturnBadRequest() throws Exception {
        String invalidJson = """
        {
            "walletUUID": "d0242b5f-96c9-4806-a924-61accf69de55",
            "operationType": "WITHDRAW"
        }
        """;

        ErrorResponse expectedResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "amount: amount is required");

        mockMvc.perform(
                        post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(expectedResponse.getCode()))
                .andExpect(jsonPath("$.message").value(expectedResponse.getMessage()));

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void walletUuidNotFound_ShouldReturnBadRequest() throws Exception {
        String invalidJson = """
        {
            "operationType": "WITHDRAW",
            "amount": 1
        }
        """;

        ErrorResponse expectedResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "walletUUID: walletUUID is required");

        mockMvc.perform(
                        post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(expectedResponse.getCode()))
                .andExpect(jsonPath("$.message").value(expectedResponse.getMessage()));

        verify(walletService, never()).executeOperation(any(), any(), any());
    }

    @Test
    void operationTypeNotFound_ShouldReturnBadRequest() throws Exception {
        String invalidJson = """
        {
            "walletUUID": "d0242b5f-96c9-4806-a924-61accf69de55",
            "amount": 1
        }
        """;

        ErrorResponse expectedResponse = new ErrorResponse(
                "VALIDATION_ERROR",
                "operationType: operationType is required");

        mockMvc.perform(
                        post("/api/v1/wallet")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(expectedResponse.getCode()))
                .andExpect(jsonPath("$.message").value(expectedResponse.getMessage()));

        verify(walletService, never()).executeOperation(any(), any(), any());
    }
}