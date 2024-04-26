package ru.nsu.raftstate;

import java.util.Arrays;
import java.util.Timer;

import ru.nsu.Entry;
import ru.nsu.rpc.RpcServerImpl;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;

public class CandidateState extends AbstractRaftState {
    private Timer myCurrentTimer;
    private Timer myCurrentTimerMoreFreq;

    private final int MORE_FREQ_TIMEOUT = ELECTION_TIMEOUT_MIN / 2;

    // Think this is done !!!!!!!!!
    public void onSwitching() {
        synchronized (mLock) {
            // Increment term when starting election
            persistance.setCurrentTerm(persistance.getCurrentTerm() + 1, mID);
            int term = persistance.getCurrentTerm();
            System.out.println("S" + mID + "." + term + ": switched to candidate mode.");
            testPrint("C: S" + mID + "." + term + ": go switched to candidate mode.");

//			long randomTime = mConfig.getTimeoutOverride() == -1 ?
//					((long) ((Math.random() * (ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN + 100))
//					+ ELECTION_TIMEOUT_MIN)) : mConfig.getTimeoutOverride();
            long randomTime =
                    ((long) ((Math.random() * (ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN)) + ELECTION_TIMEOUT_MIN));
            testPrint("C: time " + randomTime);
            myCurrentTimer = scheduleTimer(randomTime, mID);
            myCurrentTimerMoreFreq = scheduleTimer(MORE_FREQ_TIMEOUT, 0);

            //myCurrentTimer = scheduleTimer(ELECTION_TIMEOUT_MAX, mID);

            // Start an election:
            persistance.clearResponses();

            // =========== Brian - Stopped clearing votes because this would make them all -1 for same election
            persistance.clearResponses();
            requestVotes(term);

        }
    }

    // Think this is done !!!!!!!!!!1
    private void requestVotes(int term) {

        for (int i = 1; i <= persistance.getServersNumber(); i++) {
            // This should keep us from voiting for ourselves
            if (i == mID) {
                continue;
            }
            testPrint("C: S" + mID + "." + term + " is requesting vote from S" + i);
            remoteRequestVote(i, term, mID, mLog.getLastIndex(), mLog.getLastTerm());
        }
    }

    // @param candidate’s term
    // @param candidate requesting vote
    // @param index of candidate’s last log entry
    // @param term of candidate’s last log entry
    // @return 0, if server votes for candidate; otherwise, server's
    // current term

    // Think this is done !!!!!!!!!!1
    public VoteResult handleVoteRequest(int candidateTerm, int candidateID, int lastLogIndex, int lastLogTerm) {
        synchronized (mLock) {
            int term = persistance.getCurrentTerm();
            testPrint("C: S" + mID + "." + term + ": requestVote, received vote request from S" + candidateID + ".");

            if (candidateID == mID) {
                throw new IllegalStateException("bruh");
            }

            if (persistance.getCurrentTerm() >= candidateTerm) {
                testPrint("C: S" + mID + "." + term + ": requestVote, deny vote from S" + candidateID + "." + candidateTerm);
                return voteAgainstRequester(candidateTerm);
            }

            testPrint("C: S" + mID + "." + term + "requestVote, revert to follower mode");
            myCurrentTimer.cancel();
            myCurrentTimerMoreFreq.cancel();

            // ============= Brian - Here we are trying to get them to vote in the same election so we can't
            // clear votes
            persistance.clearResponses();
            FollowerState follower = new FollowerState();
            RpcServerImpl.setMode(follower);
            return follower.handleVoteRequest(candidateTerm, candidateID, lastLogIndex, lastLogTerm);
        }
    }

    // @param leader’s term
    // @param current leader
    // @param index of log entry before entries to append
    // @param term of log entry before entries to append
    // @param entries to append (in order of 0 to append.length-1)
    // @param index of highest committed entry
    // @return 0, if server appended entries; otherwise, server's
    // current term

    // This is done !!!!!!!!!!!!
    public AppendResult handleAppendEntriesRequest(int leaderTerm, int leaderID, int prevLogIndex, int prevLogTerm,
                                                   Entry[] entries,
                                                   int leaderCommit) {
        synchronized (mLock) {
            int term = persistance.getCurrentTerm();


            if (leaderTerm >= term) {
                // ===== Brian - removed this for consistency
                persistance.setCurrentTerm(leaderTerm, 0);
                myCurrentTimer.cancel();
                myCurrentTimerMoreFreq.cancel();
                persistance.clearResponses();
                // ============= Brian - added this to not waste a call
                FollowerState follower = new FollowerState();
                RpcServerImpl.setMode(follower);
                return follower.handleAppendEntriesRequest(leaderTerm, leaderID, prevLogIndex, prevLogTerm, entries,
                        leaderCommit);
            }

            return failureAppend();
        }
    }

    // @param id of the timer that timed out

    // Think this is done !!!!!!!!!!

    public void handleTimeout(int timerID) {
        synchronized (mLock) {

            //myCurrentTimer.cancel();

            int term = persistance.getCurrentTerm();
            int numServers = persistance.getServersNumber();
            var votes = persistance.getVoteResponses();

            testPrint("C: S" + mID + "." + term + ": timeout, current votes: " + votes);

            int votesFor = 1;
            for (VoteResult vote : votes.values()) {
                if (vote.isVoteGranted()) {
                    ++votesFor;
                }
            }

            // Won the election -> become leader
            if (votesFor > numServers / 2.0) {
                testPrint("C: S" + mID + "." + term + "timeout,  wins election!");
                persistance.clearResponses();
                myCurrentTimer.cancel();
                myCurrentTimerMoreFreq.cancel();
                RpcServerImpl.setMode(new LeaderState());
                return;
            }
            // Didnt win -> stay candidate
            // Brian - This goes against sudo code but TA said to do this and if receives
            // heartbeat go back to follower

            if (timerID == 0) {
                myCurrentTimerMoreFreq.cancel();
                myCurrentTimerMoreFreq = scheduleTimer(MORE_FREQ_TIMEOUT, 0);
            } else {
                persistance.clearResponses();
                testPrint("C: S" + mID + "." + term + "timeout,  didn't win... reverting back to candidate!");
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
