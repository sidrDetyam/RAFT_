package ru.nsu;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class RaftConfig {

    private final int serversNumber;
    private int currentTerm = 0;
    private int votedFor = 0;

    public void setCurrentTerm(int term, int votedFor) {
        if (term > currentTerm) {
            currentTerm = term;
            this.votedFor = votedFor;
        }
    }

    public String toString() {
        return "CURRENT_TERM =" +
                currentTerm +
                ", VOTED_FOR" +
                "=" +
                votedFor;
    }
}

