package ru.nsu.raftstate.communication;

import lombok.RequiredArgsConstructor;
import ru.nsu.raftstate.Persistence;
import ru.nsu.rpc.client.RaftRpcClient;

@RequiredArgsConstructor
public abstract class AbstractRequestTask<T> implements Runnable {
    protected final int rank;
    protected final int round;
    protected final int term;
    protected final Persistence persistence;
    protected final T requestDto;
    protected final RaftRpcClient raftRpcClient;
}
