package ru.nsu.statemachine.dto;

import java.io.Serializable;

public record VoteResult(int term, boolean voteGranted) implements Serializable {
}
