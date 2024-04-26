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

    // Think this is done !!!!!!!!!
    public void onSwitching() {
        synchronized (raftStateLock) {
            persistence.setCurrentTerm(persistence.getCurrentTerm() + 1, Optional.of(selfRank));
            System.out.println("S" + selfRank + "." + persistence.getCurrentTerm() + ": switched to candidate mode.");
            testPrint("C: S" + selfRank + "." + persistence.getCurrentTerm() + ": go switched to candidate mode.");

            long randomTime = getTimeout();
            testPrint("C: time " + randomTime);
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
            testPrint("C: S" + selfRank + "." + term + " is requesting vote from S" + i);
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
            int term = persistence.getCurrentTerm();
            testPrint("C: S" + selfRank + "." + term + ": requestVote, received vote request from S" + candidateID +
                    ".");

            if (candidateID == selfRank) {
                throw new IllegalStateException("bruh");
            }

            if (persistence.getCurrentTerm() >= candidateTerm) {
                testPrint("C: S" + selfRank + "." + term + ": requestVote, deny vote from S" + candidateID + "." + candidateTerm);
                return voteAgainstRequester(candidateTerm);
            }

            testPrint("C: S" + selfRank + "." + term + "requestVote, revert to follower mode");
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

            testPrint("C: S" + selfRank + "." + term + ": timeout, current votes: " + votes);

            int votesFor = 1;
            for (VoteResult vote : votes.values()) {
                if (vote.isVoteGranted()) {
                    ++votesFor;
                }
            }

            // Won the election -> become leader
            if (votesFor > numServers / 2.0) {
                testPrint("C: S" + selfRank + "." + term + "timeout,  wins election!");
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
                testPrint("C: S" + selfRank + "." + term + "timeout,  didn't win... reverting back to candidate!");
                myCurrentTimerMoreFreq.cancel();
                myCurrentTimer.cancel();
                onSwitching();
            }
        }

    }

    private void testPrint(String s) {
        System.out.println(s);
    }
}
