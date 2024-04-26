package ru.nsu.raftstate;

import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;

import ru.nsu.Entry;
import ru.nsu.Persistence;
import ru.nsu.RaftLog;
import ru.nsu.raftstate.communication.AppendRequestTask;
import ru.nsu.raftstate.communication.VoteRequestTask;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.dto.AppendRequestDto;
import ru.nsu.rpc.dto.VoteRequestDto;

public abstract class AbstractRaftState implements RaftState {
    protected static Persistence persistence;
    protected static RaftLog raftLog;
    protected static int selfCommitIndex;
    protected static int selfLastApplied;
    protected static int selfRank;
    protected static RaftState raftState;
    public static final String raftStateLock;

    static {
        raftStateLock = "raftStateLock";
    }

    protected final static int ELECTION_TIMEOUT_MIN = 150;
    protected final static int ELECTION_TIMEOUT_MAX = 300;
    protected final static int HEARTBEAT_INTERVAL = 75;

    public static void init(int rank, int size) {
        persistence = new Persistence(size);
        raftLog = new RaftLog();
        selfCommitIndex = 0;
        selfLastApplied = 0;
        selfRank = rank;
    }

    public static <T> T executeStateSync(Function<RaftState, ? extends T> raftStateExecutor) {
        synchronized (raftStateLock) {
            return raftStateExecutor.apply(raftState);
        }
    }

    public static void switchState(RaftState newState) {
        synchronized (raftStateLock) {
            raftState = newState;
            newState.onSwitching();
        }
    }

    protected boolean isUpToDate(int lastLogIndex, int lastLogTerm) {
        return raftLog.getLastTerm() > lastLogTerm ||
                raftLog.getLastTerm() == lastLogTerm && raftLog.getLastIndex() > lastLogIndex;
    }

    protected boolean isTermGreater(int otherTerm) {
        return persistence.getCurrentTerm() > otherTerm;
    }

    protected VoteResult voteForRequester(int candidateID, int candidateTerm) {
        persistence.setCurrentTerm(candidateTerm, Optional.of(candidateID));
        return new VoteResult(persistence.getCurrentTerm(), true);
    }

    protected long getTimeout() {
        return (long) ((Math.random() * (ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN)) + ELECTION_TIMEOUT_MIN);
    }

    protected VoteResult voteAgainstRequester(int candidateTerm) {
        persistence.setCurrentTerm(candidateTerm, Optional.empty());
        return new VoteResult(persistence.getCurrentTerm(), false);
    }

    protected AppendResult successfulAppend() {
        return new AppendResult(persistence.getCurrentTerm(), true);
    }

    protected AppendResult failureAppend() {
        return new AppendResult(persistence.getCurrentTerm(), false);
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

    protected final void remoteAppendEntries(final int rank,
                                             final int leaderTerm,
                                             final int leaderID,
                                             final int prevLogIndex,
                                             final int prevLogTerm,
                                             final Entry[] entries,
                                             final int leaderCommit) {
        synchronized (AbstractRaftState.raftStateLock) {
            int round = persistence.increaseRoundForRank(rank);
            persistence.addTask(new AppendRequestTask(rank, round, leaderTerm, persistence, new AppendRequestDto(
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
