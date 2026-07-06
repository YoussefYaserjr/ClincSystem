package com.clinicsystem.exception;

public class SlotAlreadyBookedException extends RuntimeException {
    public SlotAlreadyBookedException(String message) { super(message); }
}