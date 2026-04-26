package com.connectionsgame.abstract_class;

public abstract class Response {

    public static final String STATUS_OK = "ok";
    public static final String STATUS_ERROR = "error";

    private final String status;
    private int errorCode;
    private String errorMessage;

    protected Response() {this.status = STATUS_OK;}

    protected Response(int errorCode, String errorMessage){
        this.status = STATUS_ERROR;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getStatus() { return status; }
    public int getErrorCode() { return errorCode;}
    public String getErrorMessage() { return errorMessage; }
    public boolean isOk() { return STATUS_OK.equals(status); }

}
