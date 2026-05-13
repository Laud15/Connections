package com.connectionsgame.requests;

import com.connectionsgame.abstract_class.Request;


public class UpdateCredentialsRequest extends Request {

    private final String oldName;
    private final String oldPsw;
    private final String newName; // null if not changing
    private final String newPsw;  // null if not changing

    public UpdateCredentialsRequest(String oldName, String oldPsw, String newName, String newPsw) {
        super("updateCredentials");
        this.oldName = oldName;
        this.oldPsw  = oldPsw;
        this.newName = newName;
        this.newPsw  = newPsw;
    }

    public String getOldName() { return oldName; }
    public String getOldPsw()  { return oldPsw;  }
    public String getNewName() { return newName; }
    public String getNewPsw()  { return newPsw;  }
}