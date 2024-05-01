package ru.nsu.rpc;

import ru.nsu.raftstate.dto.ClientCommandResult;
import ru.nsu.rpc.dto.AppendRequestDto;
import ru.nsu.rpc.dto.VoteRequestDto;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.dto.client.ClientRequest;

public interface RpcServer {

    VoteResult handleVoteRequest(VoteRequestDto request);

    AppendResult handleAppendEntriesRequest(AppendRequestDto request);

    ClientCommandResult handleClient(ClientRequest clientRequest);
}
