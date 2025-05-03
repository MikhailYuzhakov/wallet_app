package ru.yuzhakov.app_wallet.controller;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.yuzhakov.app_wallet.dto.ErrorResponse;
import ru.yuzhakov.app_wallet.dto.OperationType;
import ru.yuzhakov.app_wallet.exceptions.InsufficientFundsException;
import ru.yuzhakov.app_wallet.exceptions.WalletNotFoundException;

import java.util.UUID;

@ControllerAdvice
public class RestExceptionsController {

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWalletNotFoundException(WalletNotFoundException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("WALLET_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFundsException(InsufficientFundsException ex) {
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("WALLET_ERROR", ex.getMessage()));
    }

    // Валидация полей класса DTO WalletOperation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", errorMessage));
    }

    // Обработка некорректного JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonParseException(HttpMessageNotReadableException ex) {
        String errorMessage = "Invalid JSON format";
        if (ex.getCause() instanceof JsonParseException) {
            errorMessage = "Malformed JSON";
        } else if (ex.getCause() instanceof InvalidFormatException ife) {
            if (ife.getTargetType() == OperationType.class) {
                errorMessage = "operationType must be either 'DEPOSIT' or 'WITHDRAW'";
            } else if (ife.getTargetType() == Long.class) {
                errorMessage = "amount must be a number";
            } else if (ife.getTargetType() == UUID.class) {
                errorMessage = "walletUUID must be a valid UUID format";
            }
        }

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("INVALID_REQUEST", errorMessage));
    }
}