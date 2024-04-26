package ru.nsu.raftstate;

import java.util.Arrays;
import java.util.Optional;
import java.util.Timer;

import ru.nsu.Entry;
import ru.nsu.rpc.RpcServerImpl;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;

public class FollowerState extends AbstractRaftState {
    Timer myCurrentTimer;

    public void onSwitching() {
        synchronized (raftStateLock) {
            // Set this to current term in the case that it switched from another
            int term = persistence.getCurrentTerm();
            System.out.println("S" + selfRank + "." + term + ": switched to follower mode.");
            testPrint("F: S" + selfRank + "." + term + ": switched to follower mode.");
            resetTimer();
            persistence.setVotedFor(Optional.empty());
        }
    }

    @Override
    public VoteResult handleVoteRequest(int candidateTerm, int candidateID, int lastLogIndex, int lastLogTerm) {
        synchronized (raftStateLock) {
            testPrint("F: S" + selfRank + "." + persistence.getCurrentTerm() + ": go, received vote request from S" + candidateID + ".");

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
                                                   Entry[] entries,
                                                   int leaderCommit) {
        synchronized (raftStateLock) {
            testPrint("F: S" + selfRank + "." + persistence.getCurrentTerm() + ": received  append request from S" + leaderID + "." + leaderTerm + "-PrevLogTerm:" + prevLogTerm + "-PrevLogIndex:" + prevLogIndex);

            if (isTermGreater(leaderTerm)) {
                testPrint("F: S" + selfRank + "." + persistence.getCurrentTerm() + " ignored append RPC");
                return failureAppend();
            }

            resetTimer();
            if (leaderTerm > persistence.getCurrentTerm()) {
                persistence.setCurrentTerm(leaderTerm, Optional.of(leaderID));
            }

            if (raftLog.isInconsistent(prevLogIndex, prevLogTerm)) {
                return failureAppend();
            }

            int currentTerm = persistence.getCurrentTerm();
            testPrint("F: S" + selfRank + "." + currentTerm + " accepted append RPC from S" + leaderID + "." + leaderTerm);

            testPrint("F: S" + selfRank + "." + currentTerm + " received entries " + Arrays.toString(entries));


            testPrint("F: S" + selfRank + "." + currentTerm + " current entries " + Arrays.toString(getEntries()));

            // Brian - Added this in order to check if entries actually appended
            if (!raftLog.insert(Arrays.asList(entries), prevLogIndex + 1, prevLogTerm)) {
                testPrint("F: S" + selfRank + "." + currentTerm + ": Error in appending Entries !!!!!!!!!!!!");
            }

            // Updates commit index of server
            if (leaderCommit > selfCommitIndex) {
                selfCommitIndex = Math.min(leaderCommit, raftLog.getLastIndex());
            }

            testPrint("F: S" + selfRank + "." + currentTerm + ": after append current entries " + Arrays.toString(getEntries()));


            testPrint("F: S" + selfRank + "." + currentTerm + " responded 0 for append RPC");
            return successfulAppend();
        }
    }

    public void handleTimeout(int timerID) {
        synchronized (raftStateLock) {
            int term = persistence.getCurrentTerm();
            testPrint("F: S" + selfRank + "." + term + "timeout, switching to candidate mode");
            myCurrentTimer.cancel();
            switchState(new CandidateState());
        }
    }

    private void resetTimer() {
        if (myCurrentTimer != null) {
            myCurrentTimer.cancel();
        }

        long randomTime = getTimeout();
        testPrint("F: time " + randomTime);
        myCurrentTimer = scheduleTimer(randomTime, selfRank);

    }

    private void testPrint(String s) {
        System.out.println(s);
    }

    private Entry[] getEntries() {
        if (raftLog.getLastIndex() == -1) {
            return new Entry[0];
        }

        Entry[] myEntries = new Entry[raftLog.getLastIndex() + 1];
        for (int i = 0; i <= raftLog.getLastIndex(); i++) {
            myEntries[i] = raftLog.getEntry(i);
        }
        return myEntries;
    }
}
