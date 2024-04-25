package ru.nsu;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

import ru.nsu.rpc.RaftRpcClientImpl;
import ru.nsu.rpc.RpcException;

public abstract class RaftMode {
    // config containing the latest term the server has seen and
    // candidate voted for in current term; 0, if none
    protected static RaftConfig mConfig;
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
        mConfig = config;
        mLog = log;
        mCommitIndex = 0;
        mLastApplied = lastApplied;
        mLock = new Object();
        mID = id;

        System.out.println("S" +
                mID +
                "." +
                mConfig.getCurrentTerm() +
                ": Log " +
                mLog);
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
                RaftMode.this.handleTimeout(timerID);
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

        synchronized (RaftMode.mLock) {
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
                    synchronized (RaftMode.mLock) {
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

        synchronized (RaftMode.mLock) {
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
                    synchronized (RaftMode.mLock) {
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

    // called to activate the mode
    abstract public void go();

    // @param candidate’s term
    // @param candidate requesting vote
    // @param index of candidate’s last log entry
    // @param term of candidate’s last log entry
    // @return 0, if server votes for candidate; otherwise, server's
    // current term
    abstract public int requestVote(int candidateTerm,
                                    int candidateID,
                                    int lastLogIndex,
                                    int lastLogTerm);

    // @param leader’s term
    // @param current leader
    // @param index of log entry before entries to append
    // @param term of log entry before entries to append
    // @param entries to append (in order of 0 to append.length-1)
    // @param index of highest committed entry
    // @return 0, if server appended entries; otherwise, server's
    // current term
    abstract public int appendEntries(int leaderTerm,
                                      int leaderID,
                                      int prevLogIndex,
                                      int prevLogTerm,
                                      Entry[] entries,
                                      int leaderCommit);

    // @param id of the timer that timed out
    abstract public void handleTimeout(int timerID);
}

  
