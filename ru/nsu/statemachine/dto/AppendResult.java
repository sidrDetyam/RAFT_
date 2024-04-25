package ru.nsu.statemachine.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppendResult implements Serializable {
    private int term;
    private boolean success;
}
