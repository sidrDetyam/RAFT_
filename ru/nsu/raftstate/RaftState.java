package ru.nsu.raftstate;

import ru.nsu.Entry;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;

public interface RaftState {
    void onSwitching();

    // @param candidate’s term
    // @param candidate requesting vote
    // @param index of candidate’s last log entry
    // @param term of candidate’s last log entry
    // @return 0, if server votes for candidate; otherwise, server's
    // current term
    VoteResult handleVoteRequest(
            int candidateTerm,
            int candidateID,
            int lastLogIndex,
            int lastLogTerm
    );

    // @param leader’s term
    // @param current leader
    // @param index of log entry before entries to append
    // @param term of log entry before entries to append
    // @param entries to append (in order of 0 to append.length-1)
    // @param index of highest committed entry
    // @return 0, if server appended entries; otherwise, server's
    // current term
    AppendResult handleAppendEntriesRequest(int leaderTerm,
                                            int leaderID,
                                            int prevLogIndex,
                                            int prevLogTerm,
                                            Entry[] entries,
                                            int leaderCommit);

    // @param id of the timer that timed out
    void handleTimeout(int timerID);
}
