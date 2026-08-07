/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmbpm.exception;

public class S3Exception extends RuntimeException {
    public S3Exception(String message) {
        super(message);
    }
}
