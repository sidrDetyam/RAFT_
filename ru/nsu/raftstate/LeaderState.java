package ru.nsu.raftstate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;

import ru.nsu.Entry;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;

public class LeaderState extends AbstractRaftState {
    private Timer myCurrentTimer;
    private final List<Integer> nextIndex = new ArrayList<>();

    public void onSwitching() {
        synchronized (raftStateLock) {
            myCurrentTimer = scheduleTimer(HEARTBEAT_INTERVAL, selfRank);
            initNextIndex();

            persistence.clearResponses();
            for (int i = 1; i <= persistence.getServersNumber(); i++) {
                if (i == selfRank) {
                    continue;
                }
                remoteAppendEntries(i, persistence.getCurrentTerm(), selfRank, nextIndex.get(i) - 1,
                        raftLog.getLastTerm(),
                        raftLog.getEntries(), selfCommitIndex);
            }
        }
    }

    private void initNextIndex() {
        nextIndex.clear();
        for (int i = 0; i <= persistence.getServersNumber(); ++i) {
            nextIndex.add(raftLog.getLastIndex() + 1);
        }
    }

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

    public void handleTimeout(int timerID) {
        synchronized (raftStateLock) {
            myCurrentTimer.cancel();
            int term = persistence.getCurrentTerm();
            var responses = persistence.getAppendResponses();
            myCurrentTimer = scheduleTimer(HEARTBEAT_INTERVAL, selfRank);
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
                    System.out.println(".... here");
                    if (!responses.get(rank).isSuccess()) {
                        nextIndex.set(rank, nextIndex.get(rank) - 1);
                    } else {
                        nextIndex.set(rank, raftLog.getLastIndex() + 1);
                    }
                }

//                nextIndex.set(rank, 0);
                List<Entry> newEntries = new ArrayList<>();
                for (int i = nextIndex.get(rank); i <= raftLog.getLastIndex(); i++) {
                    newEntries.add(raftLog.getEntry(i));
                }
                System.out.println(".... %s %s".formatted(newEntries, nextIndex.get(rank)));


                // TODO: Check with TA but added the -1 to indicate the one before where they will be added following
                //  Fig 2
//				Entry lastEntry = mLog.getEntry(nextIndex[rank] - 1);

//                int lastEntryTerm;
//				if (nextIndex[rank] - 1 < 0){
//                lastEntryTerm = 0;
//				}
//				else {
//					lastEntryTerm = lastEntry.term;
//				}

                remoteAppendEntries(rank, persistence.getCurrentTerm(), selfRank, nextIndex.get(rank) - 1,
                        raftLog.getLastTerm(),
                        newEntries,
                        selfCommitIndex);
            }
        }
    }
}
