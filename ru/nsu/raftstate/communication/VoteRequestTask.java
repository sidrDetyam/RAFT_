package ru.nsu.raftstate.communication;

import ru.nsu.Persistence;
import ru.nsu.raftstate.AbstractRaftState;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.RaftRpcClientImpl;
import ru.nsu.rpc.RpcException;
import ru.nsu.rpc.dto.VoteRequestDto;

public class VoteRequestTask extends AbstractRequestTask<VoteRequestDto> {

    public VoteRequestTask(int rank, int round, int term, Persistence persistence, VoteRequestDto requestDto) {
        super(rank, round, term, persistence, requestDto);
    }

    @Override
    public void run() {
        try {
            VoteResult voteResult = RaftRpcClientImpl.requestVote(rank, requestDto);
            synchronized (AbstractRaftState.raftStateLock) {
                persistence.setVoteResponse(rank, round, term, voteResult);
            }
        } catch (RpcException e) {
//                    e.printStackTrace();
        }
    }
}
