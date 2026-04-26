package com.connectionsgame;

import com.connectionsgame.abstract_class.Request;
import com.connectionsgame.requests.*;
import com.connectionsgame.responses.ErrorResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class RequestDeserializer {

    private static final Gson GSON = new Gson();

    public static Request parse(String json){

        JsonObject obj;

        try{
            obj = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e){
            throw new IllegalArgumentException("Malformed JSON: " + e.getMessage());
        }

        if (!obj.has("operation")){
            throw new IllegalArgumentException("Missing 'operation' field in request");
        }

        String operation = obj.get("operation").getAsString();

        return switch (operation) {
            case "register" -> GSON.fromJson(json, RegisterRequest.class);
            case "updateCredentials" -> GSON.fromJson(json, UpdateCredentialsRequest.class);
            case "login" -> GSON.fromJson(json, LoginRequest.class);
            case "logout" -> GSON.fromJson(json, LogoutRequest.class);
            case "submitProposal" -> GSON.fromJson(json, SubmitProposalRequest.class);
            case "requestGameInfo" -> GSON.fromJson(json, GameInfoRequest.class);
            case "requestGameStats" -> GSON.fromJson(json, GameStatsRequest.class);
            case "requestLeaderboard" -> GSON.fromJson(json, LeaderBoardRequest.class);
            case "requestPlayerStats" -> GSON.fromJson(json, PlayerStatsRequest.class);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        };
    }

}
