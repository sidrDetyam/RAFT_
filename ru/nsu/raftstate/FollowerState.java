package ru.nsu.raftstate;

import java.util.List;
import java.util.Optional;
import java.util.Timer;

import ru.nsu.Entry;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;

public class FollowerState extends AbstractRaftState {
    private Timer myCurrentTimer;

    @Override
    public void onSwitching() {
        synchronized (raftStateLock) {
            failAllRequestsOnSwitch();
            resetTimer();
            persistence.setVotedFor(Optional.empty());
        }
    }

    @Override
    public VoteResult handleVoteRequest(int candidateTerm, int candidateID, int lastLogIndex, int lastLogTerm) {
        synchronized (raftStateLock) {
            if (isTermGreater(candidateTerm)
                    || isVotedForAnother(candidateID)
                    || isUpToDate(lastLogIndex, lastLogTerm)
            ) {
                return voteAgainstRequester(candidateTerm);
            }

            return voteForRequester(candidateID, candidateTerm);
        }
    }

    private boolean isVotedForAnother(int candidateID) {
        return persistence.getVotedFor().map(vf -> vf != candidateID).orElse(false);
    }

    @Override
    public AppendResult handleAppendEntriesRequest(int leaderTerm,
                                                   int leaderID,
                                                   int prevLogIndex,
                                                   int prevLogTerm,
                                                   List<Entry> entries,
                                                   int leaderCommit) {
        synchronized (raftStateLock) {
            if (isTermGreater(leaderTerm)) {
                return failureAppend();
            }

            resetTimer();
            if (leaderTerm > persistence.getCurrentTerm()) {
                /// TODO
                persistence.setCurrentTerm(leaderTerm, Optional.of(leaderID));
            }

            if (!raftLog.isConsistent(prevLogIndex, prevLogTerm)) {
                return failureAppend();
            }

            raftLog.insert(entries, prevLogIndex, prevLogTerm);

            if (leaderCommit > selfCommitIndex) {
                selfCommitIndex = Math.min(leaderCommit, raftLog.getLastIndex());
            }
            return successfulAppend();
        }
    }

    @Override
    public void handleTimeout() {
        synchronized (raftStateLock) {
            myCurrentTimer.cancel();
            switchState(new CandidateState());
        }
    }

    private void resetTimer() {
        if (myCurrentTimer != null) {
            myCurrentTimer.cancel();
        }

        long randomTime = getTimeout();
        myCurrentTimer = scheduleTimer(randomTime);

    }
}
