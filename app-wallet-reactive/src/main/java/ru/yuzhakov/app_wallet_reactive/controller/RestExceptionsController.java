package ru.yuzhakov.app_wallet_reactive.controller;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.boot.json.JsonParseException;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;
import ru.yuzhakov.app_wallet_reactive.dto.ErrorResponse;
import ru.yuzhakov.app_wallet_reactive.dto.OperationType;
import ru.yuzhakov.app_wallet_reactive.exceptions.InsufficientFundsException;
import ru.yuzhakov.app_wallet_reactive.exceptions.WalletNotFoundException;

import java.util.UUID;

@ControllerAdvice
public class RestExceptionsController {

    // Валидация полей класса DTO WalletOperation
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(WebExchangeBindException ex) {
        return Mono.just(ex)
                .map(this::createErrorResponse)
                .map(body -> ResponseEntity.badRequest().body(body));
    }

    // Проверка структуры JSON и соответствия полей типам данных
    @ExceptionHandler(DecodingException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleJsonParseException(DecodingException ex) {

        return Mono.just(ex)
                .map(this::resolveErrorMessage)
                .map(message -> ResponseEntity.badRequest()
                        .body(new ErrorResponse("INVALID_REQUEST", message)));
    }

    // Не найден счет
    @ExceptionHandler(WalletNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWalletNotFoundException(WalletNotFoundException ex) {
        return Mono.just(ResponseEntity
                .badRequest()
                .body(new ErrorResponse("WALLET_ERROR", ex.getMessage())));
    }

    // Недостаточно средств
    @ExceptionHandler(InsufficientFundsException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInsufficientFundsException(InsufficientFundsException ex) {
        return Mono.just(ResponseEntity
                .badRequest()
                .body(new ErrorResponse("WALLET_ERROR", ex.getMessage())));
    }

    private ErrorResponse createErrorResponse(WebExchangeBindException ex) {
        String errorMessage = ex.getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Validation failed");

        return new ErrorResponse("VALIDATION_ERROR", errorMessage);
    }

    // Не валидный формат строки JSON
    private String resolveErrorMessage(DecodingException ex) {
        if (ex.getCause() instanceof JsonParseException) {
            return "Malformed JSON";
        } else if (ex.getCause() instanceof InvalidFormatException ife) {
            return resolveInvalidFormatMessage(ife);
        }
        return "Invalid JSON format";
    }

    // Не валидные поля внутри строки JSON
    private String resolveInvalidFormatMessage(InvalidFormatException ife) {
        if (ife.getTargetType() == OperationType.class) {
            return "operationType must be either 'DEPOSIT' or 'WITHDRAW'";
        } else if (ife.getTargetType() == Long.class) {
            return "amount must be a number";
        } else if (ife.getTargetType() == UUID.class) {
            return "walletUUID must be a valid UUID format";
        }
        return "Invalid field format";
    }
}
