package ru.nsu.raftstate;

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
        synchronized (raftStateLock) {
            // Increment term when starting election
            persistence.setCurrentTerm(persistence.getCurrentTerm() + 1, selfRank);
            int term = persistence.getCurrentTerm();
            System.out.println("S" + selfRank + "." + term + ": switched to candidate mode.");
            testPrint("C: S" + selfRank + "." + term + ": go switched to candidate mode.");

//			long randomTime = mConfig.getTimeoutOverride() == -1 ?
//					((long) ((Math.random() * (ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN + 100))
//					+ ELECTION_TIMEOUT_MIN)) : mConfig.getTimeoutOverride();
            long randomTime =
                    ((long) ((Math.random() * (ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN)) + ELECTION_TIMEOUT_MIN));
            testPrint("C: time " + randomTime);
            myCurrentTimer = scheduleTimer(randomTime, selfRank);
            myCurrentTimerMoreFreq = scheduleTimer(MORE_FREQ_TIMEOUT, 0);

            //myCurrentTimer = scheduleTimer(ELECTION_TIMEOUT_MAX, mID);

            // Start an election:
            persistence.clearResponses();

            // =========== Brian - Stopped clearing votes because this would make them all -1 for same election
            persistence.clearResponses();
            requestVotes(term);

        }
    }

    // Think this is done !!!!!!!!!!1
    private void requestVotes(int term) {

        for (int i = 1; i <= persistence.getServersNumber(); i++) {
            // This should keep us from voiting for ourselves
            if (i == selfRank) {
                continue;
            }
            testPrint("C: S" + selfRank + "." + term + " is requesting vote from S" + i);
            remoteRequestVote(i, term, selfRank, raftLog.getLastIndex(), raftLog.getLastTerm());
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
        synchronized (raftStateLock) {
            int term = persistence.getCurrentTerm();
            testPrint("C: S" + selfRank + "." + term + ": requestVote, received vote request from S" + candidateID + ".");

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

            // ============= Brian - Here we are trying to get them to vote in the same election so we can't
            // clear votes
            persistence.clearResponses();
            FollowerState follower = new FollowerState();
            switchState(follower);
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
        synchronized (raftStateLock) {
            int term = persistence.getCurrentTerm();


            if (leaderTerm >= term) {
                // ===== Brian - removed this for consistency
                persistence.setCurrentTerm(leaderTerm, 0);
                myCurrentTimer.cancel();
                myCurrentTimerMoreFreq.cancel();
                persistence.clearResponses();
                // ============= Brian - added this to not waste a call
                FollowerState follower = new FollowerState();
                switchState(follower);
                return follower.handleAppendEntriesRequest(leaderTerm, leaderID, prevLogIndex, prevLogTerm, entries,
                        leaderCommit);
            }

            return failureAppend();
        }
    }

    // @param id of the timer that timed out

    // Think this is done !!!!!!!!!!

    public void handleTimeout(int timerID) {
        synchronized (raftStateLock) {

            //myCurrentTimer.cancel();

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
            // Didnt win -> stay candidate
            // Brian - This goes against sudo code but TA said to do this and if receives
            // heartbeat go back to follower

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
