package ru.nsu.statemachine;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class StateMachine implements Serializable {
    private final Map<String, Object> map = new HashMap<>();
}
