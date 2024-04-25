package ru.nsu.statemachine.dto;

import java.io.Serializable;

public record AppendResult(int term, boolean success) implements Serializable {
}
