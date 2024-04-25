package ru.nsu.statemachine;

import java.util.Arrays;
import java.util.Timer;

import ru.nsu.Entry;
import ru.nsu.rpc.RpcServerImpl;
import ru.nsu.statemachine.dto.AppendResult;
import ru.nsu.statemachine.dto.VoteResult;

public class FollowerState extends AbstractRaftState {
    Timer myCurrentTimer;

    // Think this is done !!!!!!!!!
    public void onSwitching() {
        synchronized (mLock) {
            // Set this to current term in the case that it switched from another
            int term = persistance.getCurrentTerm();
            System.out.println("S" + mID + "." + term + ": switched to follower mode.");
            testPrint("F: S" + mID + "." + term + ": switched to follower mode.");
            resetTimer();
            persistance.setVotedFor(0);
        }
    }

    // @param candidate’s term
    // @param candidate requesting vote
    // @param index of candidate’s last log entry
    // @param term of candidate’s last log entry
    // @return 0, if server votes for candidate; otherwise, server's
    // current term

    // Think these are done !!!!!!!!!!
    @Override
    public VoteResult handleVoteRequest(int candidateTerm, int candidateID, int lastLogIndex, int lastLogTerm) {
        synchronized (mLock) {
            testPrint("F: S" + mID + "." + persistance.getCurrentTerm() + ": go, received vote request from S" + candidateID + ".");

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
        return persistance.getVotedFor() != 0 && persistance.getVotedFor() != candidateID;
    }

    // @param leader’s term
    // @param current leader
    // @param index of log entry before entries to append
    // @param term of log entry before entries to append
    // @param entries to append (in order of 0 to append.length-1)
    // @param index of highest committed entry
    // @return 0, if server appended entries; otherwise, server's
    // current term

    // Think this is right !!!!!!!!!
    @Override
    public AppendResult handleAppendEntriesRequest(int leaderTerm,
                                                   int leaderID,
                                                   int prevLogIndex,
                                                   int prevLogTerm,
                                                   Entry[] entries,
                                                   int leaderCommit) {
        synchronized (mLock) {
            testPrint("F: S" + mID + "." + persistance.getCurrentTerm() + ": received  append request from S" + leaderID + "." + leaderTerm + "-PrevLogTerm:" + prevLogTerm + "-PrevLogIndex:" + prevLogIndex);

            if (isTermGreater(leaderTerm)) {
                testPrint("F: S" + mID + "." + persistance.getCurrentTerm() + " ignored append RPC");
                return failureAppend();
            }

            resetTimer();
            if (leaderTerm > persistance.getCurrentTerm()) {
                persistance.setCurrentTerm(leaderTerm, leaderID);
            }

            if (mLog.isInconsistent(prevLogIndex, prevLogTerm)) {
                return failureAppend();
            }

            int currentTerm = persistance.getCurrentTerm();
            testPrint("F: S" + mID + "." + currentTerm + " accepted append RPC from S" + leaderID + "." + leaderTerm);

            testPrint("F: S" + mID + "." + currentTerm + " received entries " + Arrays.toString(entries));


            testPrint("F: S" + mID + "." + currentTerm + " current entries " + Arrays.toString(getEntries()));

            // Brian - Added this in order to check if entries actually appended
            if (!mLog.insert(Arrays.asList(entries), prevLogIndex + 1, prevLogTerm)) {
                testPrint("F: S" + mID + "." + currentTerm + ": Error in appending Entries !!!!!!!!!!!!");
            }

            // Updates commit index of server
            if (leaderCommit > mCommitIndex) {
                mCommitIndex = Math.min(leaderCommit, mLog.getLastIndex());
            }

            testPrint("F: S" + mID + "." + currentTerm + ": after append current entries " + Arrays.toString(getEntries()));


            testPrint("F: S" + mID + "." + currentTerm + " responded 0 for append RPC");
            return successfulAppend();
        }
    }

    // @param id of the timer that timed out

    // Think this is right
    public void handleTimeout(int timerID) {
        synchronized (mLock) {
            int term = persistance.getCurrentTerm();
            testPrint("F: S" + mID + "." + term + "timeout, switching to candidate mode");
            myCurrentTimer.cancel();
            RpcServerImpl.setMode(new CandidateState());
        }
    }

    private void resetTimer() {
        if (myCurrentTimer != null) {
            myCurrentTimer.cancel();
        }

//		long randomTime = mConfig.getTimeoutOverride() == -1 ? ((long) ((Math.random() * (ELECTION_TIMEOUT_MAX -
//		ELECTION_TIMEOUT_MIN + 100))
//				+ ELECTION_TIMEOUT_MIN)) : mConfig.getTimeoutOverride();
        long randomTime =
                ((long) ((Math.random() * (ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN)) + ELECTION_TIMEOUT_MIN));
        testPrint("F: time " + randomTime);
        myCurrentTimer = scheduleTimer(randomTime, mID);

    }

    private void testPrint(String s) {
        System.out.println(s);
    }

    private Entry[] getEntries() {
        if (mLog.getLastIndex() == -1) {
            return new Entry[0];
        }

        Entry[] myEntries = new Entry[mLog.getLastIndex() + 1];
        for (int i = 0; i <= mLog.getLastIndex(); i++) {
            myEntries[i] = mLog.getEntry(i);
        }
        return myEntries;
    }
}
