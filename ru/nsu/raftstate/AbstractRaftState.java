package ru.nsu.raftstate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.nsu.Entry;
import ru.nsu.Persistence;
import ru.nsu.RaftLog;
import ru.nsu.raftstate.communication.AppendRequestTask;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.ClientCommandResult;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.raftstate.statemachine.StateMachineCommand;
import ru.nsu.rpc.dto.AppendRequestDto;

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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class RequestWithCf {
        CompletableFuture<ClientCommandResult> request;
        StateMachineCommand command;
    }

    protected static final List<RequestWithCf> requests = new ArrayList<>();

    protected void failAllRequestsOnSwitch() {
        requests.stream()
                .map(RequestWithCf::getRequest)
                .forEach(cf -> cf.complete(new ClientCommandResult(false, "state switched")));
        requests.clear();
    }

    public static void init(int rank, int size) throws InterruptedException {
        persistence = new Persistence(size);

        List<Entry> initial = new ArrayList<>();
        if (rank == 1) {
//            initial.add(new Entry(0, 1));
//            initial.add(new Entry(0, 2));
//            initial.add(new Entry(0, 2));
//            initial.add(new Entry(0, 3));
//            initial.add(new Entry(0, 3));
//            initial.add(new Entry(0, 4));
        }
        raftLog = new RaftLog(initial);

        selfCommitIndex = 0;
        selfLastApplied = 0;
        selfRank = rank;

        new Thread(() -> {
            while (true) {
                try {
                    synchronized (raftStateLock) {
                        System.out.printf("%s %s %s%n",
                                Optional.ofNullable(raftState).orElse(new FollowerState()).getClass().getName(),
                                persistence.getCurrentTerm(),
                                raftLog.getEntries()
                        );
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
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

    protected final Timer scheduleTimer(long millis) {
        Timer timer = new Timer(false);
        TimerTask task = new TimerTask() {
            public void run() {
                handleTimeout();
            }
        };
        timer.schedule(task, millis);
        return timer;
    }

    protected void remoteAppendEntries(int rank,
                                       int leaderTerm,
                                       int leaderID,
                                       int prevLogIndex,
                                       int prevLogTerm,
                                       List<Entry> entries,
                                       int leaderCommit) {
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

    protected void testPrint1(String s) {
//        System.out.println(s);
    }
}
