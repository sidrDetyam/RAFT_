package ru.nsu.raftstate.communication;

import lombok.RequiredArgsConstructor;
import ru.nsu.Persistence;

@RequiredArgsConstructor
public abstract class AbstractRequestTask<T> implements Runnable {
    protected final int rank;
    protected final int round;
    protected final int term;
    protected final Persistence persistence;
    protected final T requestDto;
}
