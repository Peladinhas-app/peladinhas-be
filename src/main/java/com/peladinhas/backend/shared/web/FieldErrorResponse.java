package com.peladinhas.backend.shared.web;

public record FieldErrorResponse(
        String field,
        String message) {
}