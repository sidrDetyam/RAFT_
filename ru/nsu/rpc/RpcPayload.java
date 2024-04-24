package ru.nsu.rpc;

import java.io.Serializable;

// Можно заменить на двойную диспатчеризацию
public interface RpcPayload extends Serializable {
    int type();

    enum RpcPayloadType implements Serializable {
        RAFT_VOTE_PAYLOAD,
        RAFT_APPEND_PAYLOAD,
    }
}
