package com.connectionsgame;


import com.connectionsgame.abstract_class.Response;
import com.connectionsgame.responses.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ResponseDeserializer {

    private static final Gson GSON = new Gson();

    public static Response parse(String json){
        JsonObject obj;

        try{
            obj = JsonParser.parseString(json).getAsJsonObject();
        }catch(Exception e){
            throw new IllegalArgumentException("Malformed JSON: " + e.getMessage());
        }

        if (obj.has("status") && "error".equals(obj.get("status").getAsString())) {
            return GSON.fromJson(json, ErrorResponse.class);
        }


        if(!obj.has("operation")){
            throw new IllegalArgumentException("Missing 'operation' field in request");
        }

        String operation = obj.get("operation").getAsString();

        return switch (operation){
            case "responseGameInfo"->GSON.fromJson(json, GameInfoResponse.class);
            case "responseGameStats"->GSON.fromJson(json, GameStatsResponse.class);
            case "responseLeaderboard"->GSON.fromJson(json, LeaderBoardResponse.class);
            case "loginResponse"->GSON.fromJson(json, LoginResponse.class);
            case "logoutResponse"->GSON.fromJson(json, LogoutResponse.class);
            case "responsePlayerStats"->GSON.fromJson(json, PlayerStatsResponse.class);
            case "registerResponse"->GSON.fromJson(json, RegisterResponse.class);
            case "submitProposal"->GSON.fromJson(json, SubmitProposalResponse.class);
            case "updateCredentialsResponse"->GSON.fromJson(json, UpdateCredentialsResponse.class);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

}


