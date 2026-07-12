package com.hakotjeria.repository;

/** Kegagalan teknis pada lapisan akses data. */
public class RepositoryException extends RuntimeException {

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
