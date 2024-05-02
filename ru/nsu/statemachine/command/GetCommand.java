package ru.nsu.statemachine.command;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import ru.nsu.statemachine.StateMachine;
import ru.nsu.statemachine.StateMachineCommand;

@RequiredArgsConstructor
public class GetCommand implements StateMachineCommand {
    private final String key;

    @Override
    public Object apply(StateMachine stateMachine) {
        return Optional.ofNullable(stateMachine.getMap().get(key))
                .orElse("Value for key=%s is absent".formatted(key));
    }

    @Override
    public String toString() {
        return "g(%s)".formatted(key);
    }
}
