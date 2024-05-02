package ru.nsu.raftstate.communication;

import ru.nsu.raftstate.Persistence;
import ru.nsu.raftstate.AbstractRaftState;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.rpc.RpcException;
import ru.nsu.rpc.client.RaftRpcClient;
import ru.nsu.rpc.dto.AppendRequestDto;

public class AppendRequestTask extends AbstractRequestTask<AppendRequestDto> {


    public AppendRequestTask(int rank, int round, int term, Persistence persistence, AppendRequestDto requestDto,
                             RaftRpcClient raftRpcClient) {
        super(rank, round, term, persistence, requestDto, raftRpcClient);
    }

    @Override
    public void run() {
        try {
            AppendResult result = raftRpcClient.appendEntries(rank, requestDto);
            synchronized (AbstractRaftState.getRaftStateLock()) {
                persistence.setAppendResponse(rank, round, term, result);
            }
        } catch (RpcException e) {
//                    e.printStackTrace();
        }
    }
}
