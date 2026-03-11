package com.raizesdonordeste.exception;

import com.raizesdonordeste.api.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErrorResponse> handleEmailJaCadastrado(EmailJaCadastradoException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .erro("Conflito")
                .mensagem(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .erro("Não encontrado")
                .mensagem(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .erro("Não autorizado")
                .mensagem(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .erro("Não encontrado")
                .mensagem(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .erro("Erro de validação")
                .mensagem("Erro(s) de validação nos campos")
                .timestamp(LocalDateTime.now())
                .detalhes(errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .erro("Requisição inválida")
                .mensagem(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .erro("Erro interno")
                .mensagem("Ocorreu um erro inesperado. Entre em contato com o suporte.")
                .timestamp(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

