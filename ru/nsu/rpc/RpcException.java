package ru.nsu.rpc;

public class RpcException extends Exception {
    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
