package com.msgbridge.core;

public enum TaskStatus {
    PENDING,
    SENDING,
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    RETRYING,
    DEAD
}
