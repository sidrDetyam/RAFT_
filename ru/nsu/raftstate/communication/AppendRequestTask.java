package ru.nsu.raftstate.communication;

import ru.nsu.Persistence;
import ru.nsu.raftstate.AbstractRaftState;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.rpc.RaftRpcClientImpl;
import ru.nsu.rpc.RpcException;
import ru.nsu.rpc.dto.AppendRequestDto;

public class AppendRequestTask extends AbstractRequestTask<AppendRequestDto>{

    public AppendRequestTask(int rank, int round, int term, Persistence persistence, AppendRequestDto requestDto) {
        super(rank, round, term, persistence, requestDto);
    }

    @Override
    public void run() {
        try {
            AppendResult result = RaftRpcClientImpl.appendEntries(rank, requestDto);
            synchronized (AbstractRaftState.mLock) {
                if (!persistence.setAppend(rank, round, term, result)) {
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