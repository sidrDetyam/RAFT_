package ru.nsu.raftstate;

import java.util.Timer;
import java.util.TimerTask;

import ru.nsu.Entry;
import ru.nsu.Persistence;
import ru.nsu.RaftLog;
import ru.nsu.raftstate.communication.AppendRequestTask;
import ru.nsu.raftstate.communication.VoteRequestTask;
import ru.nsu.rpc.RaftRpcClientImpl;
import ru.nsu.rpc.RpcException;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.dto.AppendRequestDto;
import ru.nsu.rpc.dto.VoteRequestDto;

public abstract class AbstractRaftState implements RaftState {
    // config containing the latest term the server has seen and
    // candidate voted for in current term; 0, if none
    protected static Persistence persistance;
    // log containing server's entries
    protected static RaftLog mLog;
    // index of highest entry known to be committed
    protected static int mCommitIndex;
    // index of highest entry applied to state machine
    protected static int mLastApplied;
    // lock protecting access to RaftResponses
    public static Object mLock;
    // numeric id of this server
    protected static int mID;


    protected final static int ELECTION_TIMEOUT_MIN = 150;
    protected final static int ELECTION_TIMEOUT_MAX = 300;
    protected final static int HEARTBEAT_INTERVAL = 75;

    // initializes the server's mode
    public static void initializeServer(Persistence config,
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

    protected AppendResult successfulAppend() {
        return new AppendResult(persistance.getCurrentTerm(), true);
    }

    protected AppendResult failureAppend() {
        return new AppendResult(persistance.getCurrentTerm(), false);
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
        System.out.println("S" +
                src +
                "." +
                term +
                ": " + rpc +
                " for S" +
                dst +
                " failed.");

    }


    // called to make request vote RPC on another server
    // results will be stored in RaftResponses
    protected final void remoteRequestVote(final int rank,
                                           final int candidateTerm,
                                           final int candidateID,
                                           final int lastLogIndex,
                                           final int lastLogTerm) {
        synchronized (AbstractRaftState.mLock) {
            int round = persistance.increaseRoundForRank(rank);
            persistance.addTask(new VoteRequestTask(rank, round, candidateTerm, persistance, new VoteRequestDto(
                    candidateTerm,
                    candidateID,
                    lastLogIndex,
                    lastLogTerm
            )));
        }
    }

    // called to make request vote RPC on another server
    protected final void remoteAppendEntries(final int rank,
                                             final int leaderTerm,
                                             final int leaderID,
                                             final int prevLogIndex,
                                             final int prevLogTerm,
                                             final Entry[] entries,
                                             final int leaderCommit) {
        synchronized (AbstractRaftState.mLock) {
            int round = persistance.increaseRoundForRank(rank);
            persistance.addTask(new AppendRequestTask(rank, round, leaderTerm, persistance, new AppendRequestDto(
                    leaderTerm,
                    leaderID,
                    prevLogIndex,
                    prevLogTerm,
                    entries,
                    leaderCommit
            )));
        }
    }
}
