package ru.nsu.rpc;

import ru.nsu.rpc.dto.AppendRequestDto;
import ru.nsu.rpc.dto.VoteRequestDto;
import ru.nsu.statemachine.dto.VoteResult;

public interface RpcServer {

    VoteResult handleVoteRequest(VoteRequestDto request);

    int handleAppendEntriesRequest(AppendRequestDto request);
}
