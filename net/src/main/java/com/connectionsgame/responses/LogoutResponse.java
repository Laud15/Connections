package com.connectionsgame.responses;

import com.connectionsgame.abstract_class.Response;

public class LogoutResponse extends Response {

    static private final String operation = "logoutResponse";
    private final String message;

    public LogoutResponse(String message){
        super();
        this.message = message;
    }

    public String getMessage() { return message; }
}
