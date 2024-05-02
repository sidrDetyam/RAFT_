package ru.nsu.raftstate;

import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;

import ru.nsu.log.Entry;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.ClientCommandResult;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.dto.ClientRequestDto;

public class FollowerState extends AbstractRaftState {
    private Timer myCurrentTimer;
    private int leaderId;

    public FollowerState(int leaderId) {
        this.leaderId = leaderId;
    }

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
                this.leaderId = leaderID;
                persistence.setCurrentTerm(leaderTerm, Optional.of(leaderID));
            }

            if (!raftLog.isConsistent(prevLogIndex, prevLogTerm)) {
                return failureAppend();
            }

            raftLog.insert(entries, prevLogIndex, prevLogTerm);

            if (leaderCommit > selfCommitIndex) {
                selfCommitIndex = Math.min(leaderCommit, raftLog.getLastIndex());
                while (selfLastApplied < selfCommitIndex) {
                    ++selfLastApplied;
                    raftLog.getEntry(selfLastApplied).getCommand().apply(stateMachine);
                }
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

    @Override
    public CompletableFuture<ClientCommandResult> handleClientCommand(ClientRequestDto action) {
        return CompletableFuture.completedFuture(new ClientCommandResult(false,
                "Not a leader. Leader has rank %s".formatted(leaderId)));
    }

    @Override
    public String toString() {
        return "F";
    }

    private void resetTimer() {
        if (myCurrentTimer != null) {
            myCurrentTimer.cancel();
        }

        long randomTime = getTimeout();
        myCurrentTimer = scheduleTimer(randomTime);

    }
}
