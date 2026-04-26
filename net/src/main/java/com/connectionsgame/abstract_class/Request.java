package com.connectionsgame.abstract_class;

import java.util.Objects;
import com.google.gson.annotations.SerializedName;

public abstract class Request {

    protected final String operation;

    public Request(String operation){
        this.operation = Objects.requireNonNull(operation);
    }

    public String getOperation(){ return this.operation; }

}
