package ru.nsu.statemachine;

import java.io.Serializable;
import java.util.function.Function;

public interface StateMachineCommand extends Function<StateMachine, Object>, Serializable {
}
