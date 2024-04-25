package ru.nsu.rpc.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.nsu.Entry;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppendRequestDto implements Serializable {
    int leaderTerm;
    int leaderID;
    int prevLogIndex;
    int prevLogTerm;
    Entry[] entries;
    int leaderCommit;
}
