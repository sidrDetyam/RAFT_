package ru.nsu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;

@Getter
@Setter
public class Persistence {

    private final int serversNumber;
    private final Map<Integer, AppendResult> appendResponses = new HashMap<>();
    private final Map<Integer, VoteResult> voteResponses = new HashMap<>();
    private final List<Integer> rounds = new ArrayList<>();

    private final ExecutorService executorService = new ScheduledThreadPoolExecutor(4);
    private final List<Future<?>> tasks = new ArrayList<>();

    private int currentTerm = 0;
    private Optional<Integer> votedFor = Optional.empty();

    public Persistence(int serversNumber) {
        this.serversNumber = serversNumber;
        for (int i = 0; i <= serversNumber; ++i) {
            rounds.add(0);
        }
    }

    public void addTask(Runnable task) {
        tasks.add(executorService.submit(task));
    }

    public void shutdownTasks() {
        tasks.forEach(task -> task.cancel(true));
        tasks.clear();
    }

    public int increaseRoundForRank(int rank) {
        rounds.set(rank, rounds.get(rank)+1);
        return rounds.get(rank);
    }

    public void setVoteResponse(int rank,
                                int round,
                                int term,
                                VoteResult voteResult) {
        if (term == currentTerm && round == rounds.get(rank)) {
            voteResponses.put(rank, voteResult);
        }
    }

    public void setAppendResponse(int rank,
                                  int round,
                                  int term,
                                  AppendResult appendResult) {
        if (term == currentTerm && round == rounds.get(rank)) {
            appendResponses.put(rank, appendResult);
        }
    }

    public void clearResponses() {
        appendResponses.clear();
        voteResponses.clear();
    }

    public void setCurrentTerm(int term, @NonNull Optional<Integer> votedFor) {
        if (term > currentTerm) {
            currentTerm = term;
            this.votedFor = votedFor;
            clearResponses();
            shutdownTasks();
        }
    }
}

