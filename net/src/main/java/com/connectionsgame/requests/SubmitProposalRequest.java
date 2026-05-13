package com.connectionsgame.requests;

import com.connectionsgame.abstract_class.Request;

import java.util.List;


public class SubmitProposalRequest extends Request{

    private final List<String> words;

    public SubmitProposalRequest(List<String> words){
        super("submitProposal");
        this.words=words;
    }

    public List<String> getWords() { return words; }
}
