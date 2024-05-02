package ru.nsu.statemachine.command;

import lombok.RequiredArgsConstructor;
import ru.nsu.statemachine.StateMachine;
import ru.nsu.statemachine.StateMachineCommand;

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
