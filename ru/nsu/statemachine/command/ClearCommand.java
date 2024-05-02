package ru.nsu.statemachine.command;

import ru.nsu.statemachine.StateMachine;
import ru.nsu.statemachine.StateMachineCommand;

public class ClearCommand implements StateMachineCommand {
    @Override
    public Object apply(StateMachine stateMachine) {
        int size = stateMachine.getMap().size();
        stateMachine.getMap().clear();
        return "Deleted %s values".formatted(size);
    }

    @Override
    public String toString() {
        return "c";
    }
}
