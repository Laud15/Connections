package com.connectionsgame.responses;

import com.connectionsgame.abstract_class.Response;
import java.util.List;

/**
 1. Full / top-K list:
 *     { "status":"ok", "operation":"requestLeaderboard",
 *       "leaderboard": [
 *         {"rank":1,"username":"pippo","score":42},
 *         {"rank":2,"username":"beppe","score":38}
 *       ]
 *     }
 *  2. Single-player ranking:
 *     { "status":"ok", "operation":"requestLeaderboard",
 *       "username":"pippo", "score":42, "rank":1
 *     }
 */

public class LeaderBoardResponse extends Response {

    public static class LeaderboardEntry {
        private final int rank;
        private final String username;
        private final int score;

        public LeaderboardEntry(int rank, String username, int score){
            this.rank = rank;
            this.username = username;
            this.score = score;
        }

        public int getRank() { return rank; }
        public String getUsername() { return username; }
        public int getScore() { return score; }

    }


    private final String operation = "responseLeaderboard";

    //Full / top-K list
    private final List<LeaderboardEntry> leadBoard;

    //Single-player ranking
    private final String username;
    private final Integer rank;
    private Integer score;

    //list-mode constructor
    public LeaderBoardResponse(List<LeaderboardEntry> leadBoard){
        super();
        this.leadBoard = leadBoard;
        this.username = null;
        this.rank = null;
        this.score = null;
    }

    //single-player-mode constructor
    public LeaderBoardResponse(String username, int rank, int score){
        super();
        this.leadBoard = null;
        this.username = username;
        this.rank = rank;
        this.score = score;
    }

    public List<LeaderboardEntry> getLeaderboard() { return leadBoard; }
    public String  getUsername() { return username; }
    public Integer getRank()     { return rank;     }
    public Integer getScore()    { return score;    }

}
