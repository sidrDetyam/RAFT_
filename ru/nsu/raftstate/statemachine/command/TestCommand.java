package ru.nsu.raftstate.statemachine.command;

import lombok.RequiredArgsConstructor;
import ru.nsu.raftstate.statemachine.StateMachine;
import ru.nsu.raftstate.statemachine.StateMachineCommand;

@RequiredArgsConstructor
public class TestCommand implements StateMachineCommand {
    private final int action;

    @Override
    public Object apply(StateMachine stateMachine) {
        return null;
    }

    @Override
    public String toString() {
        return String.valueOf(action);
    }
}
