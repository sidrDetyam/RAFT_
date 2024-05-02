package ru.nsu.log;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import ru.nsu.statemachine.StateMachineCommand;

@Data
@AllArgsConstructor
public class Entry implements Serializable {
    private StateMachineCommand command;
    private int term;

    public Entry copy() {
        return new Entry(command, term);
    }

    @Override
    public String toString() {
        return String.format("E(t=%s; c=%s)", term, command);
    }
}
