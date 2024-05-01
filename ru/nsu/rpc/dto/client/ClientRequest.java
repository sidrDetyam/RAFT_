package ru.nsu.rpc.dto.client;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.nsu.raftstate.statemachine.StateMachineCommand;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientRequest implements Serializable {
    StateMachineCommand stateMachineCommand;
}
