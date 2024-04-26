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
                        List.of(), selfCommitIndex);
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

//	public void handleTimeout(int timerID) {
//		synchronized (mLock) {
//			myCurrentTimer.cancel();
//			int term = mConfig.getCurrentTerm();
//			int[] myResponses = RaftResponses.getAppendResponses(term);
//			myResponses = myResponses.clone();
//
//
//			testPrint("L: S" + mID + "." + term + "timeout, current entries: " + Arrays.toString(getEntries()) + "
//			resp: " + Arrays.toString(myResponses));
//
//			// ============ Brian - Moved this here to not clearAppendResponses right after sending them out
//			myCurrentTimer = scheduleTimer(HEARTBEAT_INTERVAL, mID);
//			RaftResponses.clearAppendResponses(term);
//			testPrint("L: S" + mID + "." + term + "timeout, current entries: " + Arrays.toString(getEntries()) + "
//			resp: " + Arrays.toString(myResponses));
//
//			for (int server = 1; server <= mConfig.getNumServers(); server++) {
//				if (server == mID)
//					continue;
//				// TODO: Check this with TA
//				// Brian - I added this to revert leader if it hears higher term RPC response
//				if (myResponses[server] > term){
//					mConfig.setCurrentTerm(myResponses[server], 0);
//					RaftResponses.clearAppendResponses(term);
//					RaftServerImpl.setMode(new FollowerMode());
//					return;
//				}
//				else if (myResponses[server] > 0) {
//					nextIndex[server]--;
//				}
//				else if (myResponses[server] == 0){
//					nextIndex[server] = mLog.getLastIndex() + 1;
//				}
//				// TODO: Check this added it to make sure nextIndex is updated if successful
//				int entryIter = 0;
//				Entry[] newEntries = new Entry[mLog.getLastIndex() + 1 - nextIndex[server]];
//				for (int iter = nextIndex[server]; iter <= mLog.getLastIndex(); iter++) {
//					newEntries[entryIter] = mLog.getEntry(iter);
//					entryIter++;
//				}
//
//				// TODO: Check with TA but added the -1 to indicate the one before where they will be added following
//				 Fig 2
//				Entry lastEntry = mLog.getEntry(nextIndex[server] - 1);
//
//				testPrint("L: S" + mID + "." + term + "timeout, index of last entry" + (nextIndex[server] - 1));
//				// TODO: lastEntry is sometimes null causing the following exception
//				//
//				//				Exception in thread "Timer-5" java.lang.NullPointerException
//				//				at edu.duke.raft.LeaderMode.handleTimeout(LeaderMode.java:134)
//				//				at edu.duke.raft.RaftMode$1.run(RaftMode.java:66)
//				//				at java.base/java.util.TimerThread.mainLoop(Timer.java:556)
//				//				at java.base/java.util.TimerThread.run(Timer.java:506)
//
//				int lastEntryTerm;
//				if (nextIndex[server] - 1 < 0){
//					lastEntryTerm = 0;
//				}
//				else {
//					lastEntryTerm = lastEntry.term;
//				}
//
//				remoteAppendEntries(server, mConfig.getCurrentTerm(), mID, nextIndex[server] - 1, lastEntryTerm,
//				newEntries,
//						mCommitIndex);
//			}
//		}
//	}

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
                    if (!responses.get(rank).isSuccess()) {
                        nextIndex.set(rank, nextIndex.get(rank) - 1);
                    } else {
                        nextIndex.set(rank, raftLog.getLastIndex() + 1);
                    }
                }

//                nextIndex.set(rank, 0);
                List<Entry> newEntries = new ArrayList<>();
                for (int iter = nextIndex.get(rank); iter <= raftLog.getLastIndex(); iter++) {
                    newEntries.add(raftLog.getEntry(iter));
                }


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
