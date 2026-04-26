package com.connectionsgame.server.model;

//represents a registered player
//persistence: one file per user at data/persistent/users/{username}.json
//updating a user never touches any other user's file

//puzzlesPlayed: total game joined (at least one proposal sent counts but also games where the player joined and time expired are counted)
//puzzlesCompleted: games where the player finished (won OR lost, not timed-out mid-game)
//wins: games finished with < 4 mistakes
//losses: games finished with exactly 4 mistakes
//perfectPuzzles: wins with 0 mistakes
//currentStreak: consecutive wins
//maxStreak: highest currentStreak ever reached
//mistakeHistogram: index 0..4->count of games finished with that many mistakes



public class User {

    private int user_id;
    private String username;
    private String password;
    private int totalScore;

    private int puzzlesPlayed;
    private int puzzlesCompleted;
    private int wins;
    private int losses;
    private int perfectPuzzles;
    private int currentStreak;
    private int maxStreak;
    private int[] mistakeHistogram;

    public User(int user_id, String username, String password) {
        this.user_id = user_id;
        this.username = username;
        this.password = password;
        this.totalScore = 0;
        this.mistakeHistogram = new int[5];
    }

    public int getUser_id() { return user_id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public int getTotalScore() { return totalScore; }
    public int getPuzzlesPlayed() { return puzzlesPlayed; }
    public int getPuzzlesCompleted() { return puzzlesCompleted; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
    public int getPerfectPuzzles() { return perfectPuzzles; }
    public int getCurrentStreak() { return currentStreak; }
    public int getMaxStreak() { return maxStreak; }
    public int[] getMistakeHistogram() { return mistakeHistogram; }

    public void setUsername(String username) { this.username = username;}
    public void setPassword(String password) { this.password = password; }

    //BUSINESS LOGIC
    //score: points earned in this game (always a positive value)
    //mistakes: number of wrong proposals sent (0-4)
    //won: true if the player found 3 groups before hitting 4 mistakes
    //completed: true if the player finished, false if time ran out

    public void recordGameResult(int score, int mistakes, boolean won, boolean completed){
        puzzlesPlayed++;
        totalScore += score;

        if(completed){
            puzzlesCompleted++;
            if(won){
                wins++;
                if(mistakes == 0) perfectPuzzles ++;
                currentStreak++;
                if(currentStreak > maxStreak) maxStreak = currentStreak;
            }else{
                losses++;
                currentStreak = 0;
            }
            int idx = Math.min(Math.max(mistakes, 0), 4);
            mistakeHistogram[idx]++;
        }else{
            currentStreak = 0; //timed-out without finishing: streak is broken but non counted as a loss
        }
    }

    public double getWinRate() {
        if (puzzlesPlayed == 0) return 0.0;
        return Math.round((wins * 100.0) / puzzlesPlayed);
    }

    public double getLossRate() {
        if (puzzlesPlayed == 0) return 0.0;
        return Math.round((losses * 100.0) / puzzlesPlayed);
    }

}
