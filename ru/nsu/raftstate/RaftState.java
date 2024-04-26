package ru.nsu.raftstate;

import ru.nsu.Entry;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;

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
                                            Entry[] entries,
                                            int leaderCommit);

    void handleTimeout(int timerID);
}
