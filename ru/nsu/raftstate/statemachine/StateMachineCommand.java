package ru.nsu.raftstate.statemachine;

import java.io.Serializable;
import java.util.function.Consumer;

public interface StateMachineCommand extends Consumer<StateMachine>, Serializable {
}
