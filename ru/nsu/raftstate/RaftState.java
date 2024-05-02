package ru.nsu.raftstate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import ru.nsu.log.Entry;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.ClientCommandResult;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.dto.ClientRequestDto;

public interface RaftState {
    void onSwitching();

    VoteResult handleVoteRequest(
            int candidateTerm,
            int candidateID,
            int lastLogIndex,
            int lastLogTerm
    );

    AppendResult handleAppendEntriesRequest(int leaderTerm,
                                            int leaderID,
                                            int prevLogIndex,
                                            int prevLogTerm,
                                            List<Entry> entries,
                                            int leaderCommit);

    default CompletableFuture<ClientCommandResult> handleClientCommand(ClientRequestDto action) {
        return CompletableFuture.completedFuture(new ClientCommandResult(false, "Not a leader"));
    }

    void handleTimeout();
}
