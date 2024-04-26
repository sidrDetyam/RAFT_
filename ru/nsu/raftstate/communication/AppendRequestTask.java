package ru.nsu.raftstate.communication;

import ru.nsu.Persistence;
import ru.nsu.raftstate.AbstractRaftState;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.rpc.RaftRpcClientImpl;
import ru.nsu.rpc.RpcException;
import ru.nsu.rpc.dto.AppendRequestDto;

public class AppendRequestTask extends AbstractRequestTask<AppendRequestDto> {

    public AppendRequestTask(int rank, int round, int term, Persistence persistence, AppendRequestDto requestDto) {
        super(rank, round, term, persistence, requestDto);
    }

    @Override
    public void run() {
        try {
            AppendResult result = RaftRpcClientImpl.appendEntries(rank, requestDto);
//            System.out.println(" -- apppend %s".formatted(result));
            synchronized (AbstractRaftState.raftStateLock) {
                persistence.setAppendResponse(rank, round, term, result);
            }
        } catch (RpcException e) {
//                    e.printStackTrace();
        }
    }
}
