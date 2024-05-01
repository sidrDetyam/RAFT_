package ru.nsu.raftstate.statemachine.command;

import lombok.RequiredArgsConstructor;
import ru.nsu.raftstate.statemachine.StateMachine;
import ru.nsu.raftstate.statemachine.StateMachineCommand;

@RequiredArgsConstructor
public class SetCommand implements StateMachineCommand {
    private final String key;
    private final Object value;

    @Override
    public Object apply(StateMachine stateMachine) {
        stateMachine.getMap().put(key, value);
        return stateMachine.getMap();
    }

    @Override
    public String toString(){
        return "s(%s,%s)".formatted(key, value);
    }
}
