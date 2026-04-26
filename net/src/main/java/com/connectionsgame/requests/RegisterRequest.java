package com.connectionsgame.requests;

import java.util.Objects;
import com.connectionsgame.abstract_class.Request;


public class RegisterRequest extends Request{

    private final String name;
    private final String psw;

    public RegisterRequest(String name, String psw){
        super("requestRegister");
        this.name=name;
        this.psw=psw;
    }

    public String getName() { return name; }
    public String getPsw() { return psw; }

}
