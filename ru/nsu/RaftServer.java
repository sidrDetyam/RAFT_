package ru.nsu;

import ru.nsu.rpc.dto.AppendRequestDto;
import ru.nsu.rpc.dto.VoteRequestDto;

public interface RaftServer {

    int handleVoteRequest(VoteRequestDto request);

    int handleAppendEntriesRequest(AppendRequestDto request);
}
