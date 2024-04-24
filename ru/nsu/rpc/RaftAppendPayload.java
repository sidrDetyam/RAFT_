package ru.nsu.rpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.nsu.Entry;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RaftAppendPayload implements RpcPayload {
    int leaderTerm;
    int leaderID;
    int prevLogIndex;
    int prevLogTerm;
    Entry[] entries;
    int leaderCommit;

    @Override
    public int type() {
        return RpcPayloadType.RAFT_APPEND_PAYLOAD.ordinal();
    }
}
