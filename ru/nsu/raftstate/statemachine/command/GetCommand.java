package ru.nsu.raftstate.statemachine.command;

import lombok.RequiredArgsConstructor;
import ru.nsu.raftstate.statemachine.StateMachine;
import ru.nsu.raftstate.statemachine.StateMachineCommand;

@RequiredArgsConstructor
public class GetCommand implements StateMachineCommand {
    private final String key;
    @Override
    public Object apply(StateMachine stateMachine) {
        return stateMachine.getMap().get(key);
    }

    @Override
    public String toString(){
        return "g(%s)".formatted(key);
    }
}
