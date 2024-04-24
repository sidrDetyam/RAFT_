package ru.nsu;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Entry implements Serializable {
    private int action;
    private int term;

    public Entry copy() {
        return new Entry(action, term);
    }
}
