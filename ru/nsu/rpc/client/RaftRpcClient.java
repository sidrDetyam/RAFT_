package ru.nsu.rpc.client;

import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.ClientCommandResult;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.RpcException;
import ru.nsu.rpc.dto.AppendRequestDto;
import ru.nsu.rpc.dto.ClientRequestDto;
import ru.nsu.rpc.dto.VoteRequestDto;

public interface RaftRpcClient {
    VoteResult requestVote(int rank, VoteRequestDto voteRequestDto) throws RpcException;

    AppendResult appendEntries(int rank, AppendRequestDto appendRequestDto) throws RpcException;

    ClientCommandResult clientRequest(int rank, ClientRequestDto clientRequest) throws RpcException;
}
