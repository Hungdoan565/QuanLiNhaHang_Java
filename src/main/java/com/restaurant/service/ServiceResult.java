package com.restaurant.service;

/**
 * Generic service result wrapper for consistent response handling
 */
public class ServiceResult<T> {
    
    private final boolean success;
    private final T data;
    private final String message;
    
    private ServiceResult(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }
    
    /**
     * Create success result with data and message
     */
    public static <T> ServiceResult<T> success(T data, String message) {
        return new ServiceResult<>(true, data, message);
    }
    
    /**
     * Create success result with data only
     */
    public static <T> ServiceResult<T> success(T data) {
        return new ServiceResult<>(true, data, null);
    }
    
    /**
     * Create error result with message
     */
    public static <T> ServiceResult<T> error(String message) {
        return new ServiceResult<>(false, null, message);
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getMessage() { return message; }
    
    @Override
    public String toString() {
        return String.format("ServiceResult{success=%s, message='%s'}", success, message);
    }
}
