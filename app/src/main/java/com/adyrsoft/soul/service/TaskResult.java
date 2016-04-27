package com.adyrsoft.soul.service;

/**
 * Result types used to report to clients using TaskListener on how a task finished.
 */
public enum TaskResult {
    COMPLETED,
    FAILED,
    CANCELED
}
