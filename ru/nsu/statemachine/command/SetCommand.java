package ru.nsu.statemachine.command;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import ru.nsu.statemachine.StateMachine;
import ru.nsu.statemachine.StateMachineCommand;

@RequiredArgsConstructor
public class SetCommand implements StateMachineCommand {
    private final String key;
    private final Object value;

    @Override
    public Object apply(StateMachine stateMachine) {
        String message = Optional.ofNullable(stateMachine.getMap().get(key))
                .map(oldValue -> "Value for key=%s changed: %s -> %s".formatted(key, oldValue, value))
                .orElse("Set value=%s for key=%s".formatted(value, key));
        stateMachine.getMap().put(key, value);
        return message;
    }

    @Override
    public String toString(){
        return "s(%s,%s)".formatted(key, value);
    }
}
