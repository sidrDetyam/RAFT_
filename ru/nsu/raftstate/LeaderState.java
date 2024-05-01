package ru.nsu.raftstate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;

import ru.nsu.Entry;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.ClientCommandResult;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.dto.client.ClientRequest;

public class LeaderState extends AbstractRaftState {
    private Timer myCurrentTimer;
    private final List<Integer> nextIndex = new ArrayList<>();
    private final List<Integer> matchIndex = new ArrayList<>();

    @Override
    public void onSwitching() {
        synchronized (raftStateLock) {
            failAllRequestsOnSwitch();
            myCurrentTimer = scheduleTimer(HEARTBEAT_INTERVAL);
            initNextIndex();

            persistence.clearResponses();
            for (int i = 1; i <= persistence.getServersNumber(); i++) {
                if (i == selfRank) {
                    continue;
                }
                remoteAppendEntries(i, persistence.getCurrentTerm(), selfRank, raftLog.getLastIndex(),
                        raftLog.getLastTerm(),
                        List.of(), selfCommitIndex);
            }
        }
    }

    private void initNextIndex() {
        nextIndex.clear();
        for (int i = 0; i <= persistence.getServersNumber(); ++i) {
            nextIndex.add(raftLog.getLastIndex() + 1);
            matchIndex.add(-1);
        }
    }

    @Override
    public VoteResult handleVoteRequest(int candidateTerm, int candidateID, int lastLogIndex, int lastLogTerm) {
        synchronized (raftStateLock) {
            if (persistence.getCurrentTerm() < candidateTerm) {
                myCurrentTimer.cancel();
                persistence.setCurrentTerm(candidateTerm, Optional.empty());
                persistence.clearResponses();
                FollowerState follower = new FollowerState();
                switchState(follower);
                return follower.handleVoteRequest(candidateTerm, candidateID, lastLogIndex, lastLogTerm);
            }
            return new VoteResult(persistence.getCurrentTerm(), false);
        }
    }

    @Override
    public AppendResult handleAppendEntriesRequest(int leaderTerm, int leaderID, int prevLogIndex, int prevLogTerm,
                                                   List<Entry> entries,
                                                   int leaderCommit) {
        synchronized (raftStateLock) {
            int term = persistence.getCurrentTerm();
            if (leaderTerm > term) {
                persistence.setCurrentTerm(leaderTerm, Optional.empty());
                myCurrentTimer.cancel();
                persistence.clearResponses();
                FollowerState follower = new FollowerState();
                switchState(follower);
                return follower.handleAppendEntriesRequest(leaderTerm, leaderID, prevLogIndex, prevLogTerm, entries,
                        leaderCommit);
            }

            return failureAppend();
        }
    }

    @Override
    public void handleTimeout() {
        synchronized (raftStateLock) {
            myCurrentTimer.cancel();
            int term = persistence.getCurrentTerm();
            var responses = persistence.getAppendResponses();
            myCurrentTimer = scheduleTimer(HEARTBEAT_INTERVAL);
            persistence.clearResponses();

            for (int rank = 1; rank <= persistence.getServersNumber(); rank++) {
                if (rank == selfRank) {
                    continue;
                }
                if (responses.get(rank) != null && responses.get(rank).getTerm() > term) {
                    persistence.setCurrentTerm(responses.get(rank).getTerm(), Optional.empty());
                    persistence.clearResponses();
                    switchState(new FollowerState());
                    return;
                }

                if (responses.get(rank) != null) {
                    if (!responses.get(rank).isSuccess()) {
                        nextIndex.set(rank, Math.max(nextIndex.get(rank) - 1, 0));
                    } else {
                        nextIndex.set(rank, Math.min(nextIndex.get(rank) + 1, raftLog.getLastIndex() + 1));
                        matchIndex.set(rank, Math.min(matchIndex.get(rank)+1, raftLog.getLastIndex()));
                    }
                }

                List<Entry> newEntries = new ArrayList<>();
                for (int i = nextIndex.get(rank); i <= Math.min(raftLog.getLastIndex(), nextIndex.get(rank)+1); i++) {
                    newEntries.add(raftLog.getEntry(i));
                }

                remoteAppendEntries(rank, persistence.getCurrentTerm(), selfRank, nextIndex.get(rank) - 1,
                        raftLog.getPrevTerm(nextIndex.get(rank)),
                        newEntries,
                        selfCommitIndex);

                handleRequests();
            }
        }
    }

    private long matchedAtIndex(int index) {
        return allNodesStream().filter(i -> matchIndex.get(i) >= index).count() + 1;
    }

    private void handleRequests() {
        List<RequestWithCf> unhandled = new ArrayList<>();
        requests.forEach(requestWithCf -> {
            if (isQuorum((int) matchedAtIndex(requestWithCf.index))) {
                Object details = raftLog.getEntry(requestWithCf.index).getCommand().apply(stateMachine);
                requestWithCf.request.complete(new ClientCommandResult(true, details));
                ++selfLastApplied;
                ++selfCommitIndex;
            }
            else {
                unhandled.add(requestWithCf);
            }
        });
        requests.clear();
        requests.addAll(unhandled);
    }

    @Override
    public String toString() {
        return "L";
    }

    @Override
    public CompletableFuture<ClientCommandResult> handleClientCommand(ClientRequest clientRequest) {
        synchronized (raftStateLock) {
            CompletableFuture<ClientCommandResult> cf = new CompletableFuture<>();
            raftLog.addEntry(new Entry(clientRequest.getStateMachineCommand(), persistence.getCurrentTerm()));
            requests.add(new RequestWithCf(raftLog.getLastIndex(), cf, clientRequest.getStateMachineCommand()));
            return cf;
        }
    }
}
