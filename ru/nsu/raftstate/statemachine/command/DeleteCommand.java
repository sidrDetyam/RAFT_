package ru.nsu.raftstate.statemachine.command;

import lombok.RequiredArgsConstructor;
import ru.nsu.raftstate.statemachine.StateMachine;
import ru.nsu.raftstate.statemachine.StateMachineCommand;

@RequiredArgsConstructor
public class DeleteCommand implements StateMachineCommand {
    private final String key;
    @Override
    public Object apply(StateMachine stateMachine) {
        stateMachine.getMap().remove(key);
        return stateMachine.getMap();
    }

    @Override
    public String toString(){
        return "d(%s)".formatted(key);
    }
}
