package ru.nsu.raftstate;

import java.util.Optional;
import java.util.Timer;

import ru.nsu.Entry;
import ru.nsu.raftstate.communication.VoteRequestTask;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.dto.VoteRequestDto;

public class CandidateState extends AbstractRaftState {
    private Timer myCurrentTimer;
    private Timer myCurrentTimerMoreFreq;

    private final int MORE_FREQ_TIMEOUT = ELECTION_TIMEOUT_MIN / 2;

    public void onSwitching() {
        synchronized (raftStateLock) {
            persistence.setCurrentTerm(persistence.getCurrentTerm() + 1, Optional.of(selfRank));
            long randomTime = getTimeout();
            myCurrentTimer = scheduleTimer(randomTime, selfRank);
            myCurrentTimerMoreFreq = scheduleTimer(MORE_FREQ_TIMEOUT, 0);
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
            myCurrentTimerMoreFreq.cancel();

            persistence.clearResponses();
            FollowerState follower = new FollowerState();
            switchState(follower);
            return follower.handleVoteRequest(candidateTerm, candidateID, lastLogIndex, lastLogTerm);
        }
    }

    public AppendResult handleAppendEntriesRequest(int leaderTerm, int leaderID, int prevLogIndex, int prevLogTerm,
                                                   Entry[] entries,
                                                   int leaderCommit) {
        synchronized (raftStateLock) {
            int term = persistence.getCurrentTerm();

            if (leaderTerm >= term) {
                persistence.setCurrentTerm(leaderTerm, Optional.empty());
                myCurrentTimer.cancel();
                myCurrentTimerMoreFreq.cancel();
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
            int term = persistence.getCurrentTerm();
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
                myCurrentTimerMoreFreq.cancel();
                switchState(new LeaderState());
                return;
            }

            if (timerID == 0) {
                myCurrentTimerMoreFreq.cancel();
                myCurrentTimerMoreFreq = scheduleTimer(MORE_FREQ_TIMEOUT, 0);
            } else {
                persistence.clearResponses();
                myCurrentTimerMoreFreq.cancel();
                myCurrentTimer.cancel();
                onSwitching();
            }
        }

    }
}
