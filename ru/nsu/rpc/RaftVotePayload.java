package ru.nsu.rpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RaftVotePayload implements RpcPayload {
    int candidateTerm;
    int candidateID;
    int lastLogIndex;
    int lastLogTer;

    @Override
    public int type() {
        return RpcPayloadType.RAFT_VOTE_PAYLOAD.ordinal();
    }
}
