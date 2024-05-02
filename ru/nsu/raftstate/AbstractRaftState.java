package ru.nsu.raftstate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.IntStream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.nsu.log.Entry;
import ru.nsu.log.RaftLog;
import ru.nsu.log.InMemoryRaftLog;
import ru.nsu.raftstate.communication.AppendRequestTask;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.ClientCommandResult;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.client.RaftRpcClient;
import ru.nsu.rpc.client.RaftRpcClientImpl;
import ru.nsu.statemachine.StateMachine;
import ru.nsu.statemachine.StateMachineCommand;
import ru.nsu.rpc.dto.AppendRequestDto;

public abstract class AbstractRaftState implements RaftState {
    protected static Persistence persistence;
    protected static final RaftLog raftLog = new InMemoryRaftLog();
    protected static int selfCommitIndex;
    protected static int selfLastApplied;
    protected static final StateMachine stateMachine = new StateMachine();
    protected static int selfRank;
    protected static RaftState raftState;
    @Getter
    protected static final String raftStateLock;
    protected static final RaftRpcClient raftRpcClient = new RaftRpcClientImpl();

    static {
        raftStateLock = "raftStateLock";
    }

    protected final static int ELECTION_TIMEOUT_MIN = 150;
    protected final static int ELECTION_TIMEOUT_MAX = 300;
    protected final static int HEARTBEAT_INTERVAL = 75;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RequestWithCf {
        int index;
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

    public static void init(int rank, int size) {
        persistence = new Persistence(size);
        selfCommitIndex = -1;
        selfLastApplied = -1;
        selfRank = rank;

        new Thread(() -> {
            while (true) {
                try {
                    synchronized (raftStateLock) {
                        System.out.printf("m=%s t=%s a=%s c=%s %s%n",
                                Optional.ofNullable(raftState).orElse(new FollowerState()),
                                persistence.getCurrentTerm(),
                                selfLastApplied,
                                selfCommitIndex,
//                                raftLog.getEntries()
                                stateMachine.getMap()
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

    protected IntStream allNodesStream() {
        return IntStream.range(1, persistence.getServersNumber() + 1);
    }

    protected boolean isQuorum(int num) {
        return num > persistence.getServersNumber() / 2.0;
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
            ), raftRpcClient));
        }
    }
}
