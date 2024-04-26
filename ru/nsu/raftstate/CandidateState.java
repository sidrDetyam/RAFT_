package ru.nsu.raftstate;

import java.util.List;
import java.util.Optional;
import java.util.Timer;

import ru.nsu.Entry;
import ru.nsu.raftstate.communication.VoteRequestTask;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.dto.VoteRequestDto;

public class CandidateState extends AbstractRaftState {
    private Timer myCurrentTimer;

    public void onSwitching() {
        synchronized (raftStateLock) {
            persistence.setCurrentTerm(persistence.getCurrentTerm() + 1, Optional.of(selfRank));
            long randomTime = getTimeout();
            myCurrentTimer = scheduleTimer(randomTime, selfRank);
            persistence.clearResponses();
            persistence.clearResponses();
            requestVotes(persistence.getCurrentTerm());
        }
    }

    private void requestVotes(int term) {
        for (int i = 1; i <= persistence.getServersNumber(); i++) {
            if (i == selfRank) {
                continue;
            }
            remoteRequestVote(i, term, selfRank, raftLog.getLastIndex(), raftLog.getLastTerm());
        }
    }

    private void remoteRequestVote(final int rank,
                                   final int candidateTerm,
                                   final int candidateID,
                                   final int lastLogIndex,
                                   final int lastLogTerm) {
        synchronized (AbstractRaftState.raftStateLock) {
            int round = persistence.increaseRoundForRank(rank);
            persistence.addTask(new VoteRequestTask(rank, round, candidateTerm, persistence, new VoteRequestDto(
                    candidateTerm,
                    candidateID,
                    lastLogIndex,
                    lastLogTerm
            )));
        }
    }

    public VoteResult handleVoteRequest(int candidateTerm, int candidateID, int lastLogIndex, int lastLogTerm) {
        synchronized (raftStateLock) {
            if (candidateID == selfRank) {
                throw new IllegalStateException("bruh");
            }

            if (persistence.getCurrentTerm() >= candidateTerm) {
                return voteAgainstRequester(candidateTerm);
            }

            myCurrentTimer.cancel();

            persistence.clearResponses();
            FollowerState follower = new FollowerState();
            switchState(follower);
            return follower.handleVoteRequest(candidateTerm, candidateID, lastLogIndex, lastLogTerm);
        }
    }

    public AppendResult handleAppendEntriesRequest(int leaderTerm, int leaderID, int prevLogIndex, int prevLogTerm,
                                                   List<Entry> entries,
                                                   int leaderCommit) {
        synchronized (raftStateLock) {
            int term = persistence.getCurrentTerm();

            if (leaderTerm >= term) {
                persistence.setCurrentTerm(leaderTerm, Optional.empty());
                myCurrentTimer.cancel();
                persistence.clearResponses();
                FollowerState follower = new FollowerState();
                switchState(follower);
                return follower.handleAppendEntriesRequest(leaderTerm, leaderID, prevLogIndex, prevLogTerm, entries,
                        leaderCommit);
            }

            return failureAppend();
        }
    }

    public void handleTimeout(int timerID) {
        synchronized (raftStateLock) {
            int numServers = persistence.getServersNumber();
            var votes = persistence.getVoteResponses();

            int votesFor = 1;
            for (VoteResult vote : votes.values()) {
                if (vote.isVoteGranted()) {
                    ++votesFor;
                }
            }

            if (votesFor > numServers / 2.0) {
                persistence.clearResponses();
                myCurrentTimer.cancel();
                switchState(new LeaderState());
                return;
            }

            persistence.clearResponses();
            myCurrentTimer.cancel();
            onSwitching();
        }

    }
}
