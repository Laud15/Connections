package com.connectionsgame.responses;

import com.connectionsgame.abstract_class.Response;

public class UpdateCredentialsResponse extends Response {
    private static final String operation = "updateCredentialsResponse";
    private final String message;

    public UpdateCredentialsResponse(String message){
        super();
        this.message = message;
    }

    public String getMessage() { return message; }
}
