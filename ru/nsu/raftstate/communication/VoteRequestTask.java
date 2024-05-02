package ru.nsu.raftstate.communication;

import ru.nsu.raftstate.Persistence;
import ru.nsu.raftstate.AbstractRaftState;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.RpcException;
import ru.nsu.rpc.client.RaftRpcClient;
import ru.nsu.rpc.dto.VoteRequestDto;

public class VoteRequestTask extends AbstractRequestTask<VoteRequestDto> {


    public VoteRequestTask(int rank, int round, int term, Persistence persistence, VoteRequestDto requestDto,
                           RaftRpcClient raftRpcClient) {
        super(rank, round, term, persistence, requestDto, raftRpcClient);
    }

    @Override
    public void run() {
        try {
            VoteResult voteResult = raftRpcClient.requestVote(rank, requestDto);
            synchronized (AbstractRaftState.getRaftStateLock()) {
                persistence.setVoteResponse(rank, round, term, voteResult);
            }
        } catch (RpcException e) {
//                    e.printStackTrace();
        }
    }
}
