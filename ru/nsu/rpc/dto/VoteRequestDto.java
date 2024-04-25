package ru.nsu.rpc.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VoteRequestDto implements Serializable {
    int candidateTerm;
    int candidateID;
    int lastLogIndex;
    int lastLogTerm;
}
