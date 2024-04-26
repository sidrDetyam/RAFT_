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
            synchronized (AbstractRaftState.mLock) {
                if (!persistence.setVote(rank, round, term, voteResult)) {
//                    System.err.println("RaftResponses.setVote(" +
//                            "serverID " + serverID + ", " +
//                            "response " + voteResult + ", " +
//                            "candidateTerm " + candidateTerm + ", " +
//                            "candidateRound " + mRound +
//                            ") failed.");
                }
            }
        } catch (RpcException e) {
//                    e.printStackTrace();
        }
    }
}
