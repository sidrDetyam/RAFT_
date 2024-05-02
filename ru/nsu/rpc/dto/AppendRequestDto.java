package ru.nsu.rpc.dto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.nsu.log.Entry;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppendRequestDto implements Serializable {
    private int leaderTerm;
    private int leaderID;
    private int prevLogIndex;
    private int prevLogTerm;
    private List<Entry> entries;
    private int leaderCommit;
}
