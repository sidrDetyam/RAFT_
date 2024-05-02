package ru.nsu.rpc.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.nsu.statemachine.StateMachineCommand;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientRequestDto implements Serializable {
    private StateMachineCommand stateMachineCommand;
}
