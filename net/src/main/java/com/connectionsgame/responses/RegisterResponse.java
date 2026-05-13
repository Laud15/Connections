package com.connectionsgame.responses;
import com.connectionsgame.abstract_class.Response;


public class RegisterResponse extends Response {

    private final String operation = "registerResponse";
    private final String message;

    public RegisterResponse(String message) {
        super();
        this.message = message;
    }

    public String getMessage() { return message; }
}