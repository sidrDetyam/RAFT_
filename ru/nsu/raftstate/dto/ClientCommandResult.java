package ru.nsu.raftstate.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientCommandResult implements Serializable {
    private boolean success;
    private Object details;

    @Override
    public String toString() {
        return "Success: %s, details: %s".formatted(success, details);
    }
}
