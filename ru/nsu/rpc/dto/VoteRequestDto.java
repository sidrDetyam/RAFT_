package ru.nsu.rpc.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoteRequestDto implements Serializable {
    private int candidateTerm;
    private int candidateID;
    private int lastLogIndex;
    private int lastLogTerm;
}
