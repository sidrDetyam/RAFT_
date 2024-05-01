package ru.nsu.raftstate.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientCommandResult implements Serializable {
    boolean success;
    String message;
}
