package com.connectionsgame.responses;

import com.connectionsgame.abstract_class.Response;

/**
 * A generic error response.
 *
 * Serialized form (example):
 * {
 *   "status": "error",
 *   "errorCode": 401,
 *   "errorMessage": "Wrong username or password"
 * }
 */


public class ErrorResponse extends Response {

    public ErrorResponse(int code, String message) { super(code, message);}

}
