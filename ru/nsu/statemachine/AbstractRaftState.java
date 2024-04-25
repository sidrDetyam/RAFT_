package ru.nsu.statemachine;

import java.util.Timer;
import java.util.TimerTask;

import ru.nsu.Entry;
import ru.nsu.RaftConfig;
import ru.nsu.RaftLog;
import ru.nsu.RaftResponses;
import ru.nsu.rpc.RaftRpcClientImpl;
import ru.nsu.rpc.RpcException;
import ru.nsu.statemachine.dto.VoteResult;

public abstract class AbstractRaftState implements RaftState {
    // config containing the latest term the server has seen and
    // candidate voted for in current term; 0, if none
    protected static RaftConfig persistance;
    // log containing server's entries
    protected static RaftLog mLog;
    // index of highest entry known to be committed
    protected static int mCommitIndex;
    // index of highest entry applied to state machine
    protected static int mLastApplied;
    // lock protecting access to RaftResponses
    protected static Object mLock;
    // numeric id of this server
    protected static int mID;


    protected final static int ELECTION_TIMEOUT_MIN = 150;
    protected final static int ELECTION_TIMEOUT_MAX = 300;
    protected final static int HEARTBEAT_INTERVAL = 75;

    // initializes the server's mode
    public static void initializeServer(RaftConfig config,
                                        RaftLog log,
                                        int lastApplied,
                                        int id) {
        persistance = config;
        mLog = log;
        mCommitIndex = 0;
        mLastApplied = lastApplied;
        mLock = new Object();
        mID = id;

        System.out.println("S" +
                mID +
                "." +
                persistance.getCurrentTerm() +
                ": Log " +
                mLog);
    }

    protected boolean isUpToDate(int lastLogIndex, int lastLogTerm) {
        return mLog.getLastTerm() > lastLogTerm ||
                mLog.getLastTerm() == lastLogTerm && mLog.getLastIndex() > lastLogIndex;
    }

    protected boolean isTermGreater(int otherTerm) {
        return persistance.getCurrentTerm() > otherTerm;
    }

    protected VoteResult voteForRequester(int candidateID, int candidateTerm) {
        persistance.setCurrentTerm(candidateTerm, candidateID);
        return new VoteResult(persistance.getCurrentTerm(), true);
    }

    protected VoteResult voteAgainstRequester(int candidateTerm) {
        persistance.setCurrentTerm(candidateTerm, 0);
        return new VoteResult(persistance.getCurrentTerm(), false);
    }

    // @param milliseconds for the timer to wait
    // @param a way to identify the timer when handleTimeout is called
    // after the timeout period
    // @return Timer object that will schedule a call to the mode's
    // handleTimeout method. If an event occurs before the timeout
    // period, then the mode should call the Timer's cancel method.
    protected final Timer scheduleTimer(long millis,
                                        final int timerID) {
        Timer timer = new Timer(false);
        TimerTask task = new TimerTask() {
            public void run() {
                AbstractRaftState.this.handleTimeout(timerID);
            }
        };
        timer.schedule(task, millis);
        return timer;
    }

    private void printFailedRPC(int src,
                                int dst,
                                int term,
                                String rpc) {
/*    System.out.println ("S" + 
			src + 
			"." +
			term + 
			": " + rpc +
			" for S" + 
			dst + 
			" failed.");
*/
    }


    // called to make request vote RPC on another server
    // results will be stored in RaftResponses
    protected final void remoteRequestVote(final int serverID,
                                           final int candidateTerm,
                                           final int candidateID,
                                           final int lastLogIndex,
                                           final int lastLogTerm) {
        final int round; // the round under which this request will be made
        int[] rounds = null;

        synchronized (AbstractRaftState.mLock) {
            if ((rounds = RaftResponses.getRounds(candidateTerm)) != null) {
                // we will check the latest round when we receive a response
                round = (rounds[serverID]) + 1;
                // update the round for the server we are contacting
                if (!RaftResponses.setRound(serverID, round, candidateTerm)) {
                    System.err.println("RaftResponses.setRound(" +
                            "serverID " + serverID +
                            "round " + round +
                            "candidateTerm " + candidateTerm +
                            ") failed.");
                    return;
                }
            } else {
                System.err.println("RaftResponses.getRounds(" +
                        "candidateTerm " + candidateTerm +
                        ") failed.");
                return;
            }
        }

        (new Thread(new Runnable() {
            private int mRound = round;

            public void run() {
                int[] rounds = null;
                try {
                    int response = RaftRpcClientImpl.requestVote(serverID, candidateTerm,
                            candidateID,
                            lastLogIndex,
                            lastLogTerm);
                    synchronized (AbstractRaftState.mLock) {
                        if (!RaftResponses.setVote(serverID,
                                response,
                                candidateTerm,
                                mRound)) {
                            System.err.println("RaftResponses.setVote(" +
                                    "serverID " + serverID + ", " +
                                    "response " + response + ", " +
                                    "candidateTerm " + candidateTerm + ", " +
                                    "candidateRound " + mRound +
                                    ") failed.");
                        }
                    }
                } catch (RpcException e) {
                    printFailedRPC(candidateID,
                            serverID,
                            candidateTerm,
                            "requestVote");
                }
            }
        })).start();

    }

    // called to make request vote RPC on another server
    protected final void remoteAppendEntries(final int serverID,
                                             final int leaderTerm,
                                             final int leaderID,
                                             final int prevLogIndex,
                                             final int prevLogTerm,
                                             final Entry[] entries,
                                             final int leaderCommit) {
        final int round; // the round under which this request will be made
        int[] rounds = null;

        synchronized (AbstractRaftState.mLock) {
            if ((rounds = RaftResponses.getRounds(leaderTerm)) != null) {
                // we will check the latest round when we receive a response
                round = (rounds[serverID]) + 1;
                // update the round for the server we are contacting
                if (!RaftResponses.setRound(serverID, round, leaderTerm)) {
                    System.err.println("RaftResponses.setRound(" +
                            "serverID " + serverID +
                            "round " + round +
                            "leaderTerm " + leaderTerm +
                            ") failed.");
                    return;
                }
            } else {
                System.err.println("RaftResponses.getRounds(" +
                        "leaderTerm " + leaderTerm +
                        ") failed.");
                return;
            }
        }

        new Thread(new Runnable() {
            private int mRound = round;

            public void run() {
                int[] rounds = null;

                try {
                    int response = RaftRpcClientImpl.appendEntries(serverID, leaderTerm,
                            leaderID,
                            prevLogIndex,
                            prevLogTerm,
                            entries,
                            leaderCommit);
                    synchronized (AbstractRaftState.mLock) {
                        if (!RaftResponses.setAppendResponse(serverID,
                                response,
                                leaderTerm,
                                mRound)) {
                            System.err.println("RaftResponses.setAppendResponse(" +
                                    "serverID " + serverID + ", " +
                                    "response " + response + ", " +
                                    "requestTerm " + leaderTerm + ", " +
                                    "requestRound " + mRound +
                                    ") failed.");
                        }
                    }
                } catch (RpcException e) {
                    printFailedRPC(leaderID,
                            serverID,
                            leaderTerm,
                            "appendEntries");
                }
            }
        }).start();
    }
}

  
