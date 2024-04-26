package ru.nsu.raftstate;

import java.util.Arrays;
import java.util.Optional;
import java.util.Timer;

import ru.nsu.Entry;
import ru.nsu.rpc.RpcServerImpl;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;

public class LeaderState extends AbstractRaftState {
    private Timer myCurrentTimer;
    private int[] nextIndex;

    // TODO: Ask TA Do we need this?
    private int[] matchIndex;

    // Think this is done !!!!!!!!!
    public void onSwitching() {
        synchronized (raftStateLock) {
            // Set this to current term in the case that it switched from another
            int term = persistence.getCurrentTerm();
            myCurrentTimer = scheduleTimer(HEARTBEAT_INTERVAL, selfRank);

            nextIndex = new int[persistence.getServersNumber() + 1];
            for (int server = 1; server <= persistence.getServersNumber(); server++) {
                nextIndex[server] = raftLog.getLastIndex() + 1;
            }

            // int term = 0;
            System.out.println("S" + selfRank + "." + term + ": switched to leader mode.");
            testPrint("L: S" + selfRank + "." + term + ": switched to leader mode.");

            // TODO: Added this dont know if we need it
            persistence.clearResponses();
            // Send Initial Heartbeats
            for (int i = 1; i <= persistence.getServersNumber(); i++) {
                // This should keep us from voting for ourselves
				if (i == selfRank) {
					continue;
				}

                remoteAppendEntries(i, persistence.getCurrentTerm(), selfRank, nextIndex[i] - 1, raftLog.getLastTerm(),
						new Entry[0], selfCommitIndex);
            }
        }
    }

    // @param candidate’s term
    // @param candidate requesting vote
    // @param index of candidate’s last log entry
    // @param term of candidate’s last log entry
    // @return 0, if server votes for candidate; otherwise, server's
    // current term
    // worked on by: Molly

    // Think this is done!!!!!!!!!!!!
    public VoteResult handleVoteRequest(int candidateTerm, int candidateID, int lastLogIndex, int lastLogTerm) {
        synchronized (raftStateLock) {
            if (persistence.getCurrentTerm() < candidateTerm) { // if their term greater, they are real leader. I become a follower
                testPrint("L: S" + selfRank + "." + persistence.getCurrentTerm() + ": reverted to follower mode");
                myCurrentTimer.cancel();
                persistence.setCurrentTerm(candidateTerm, Optional.empty());
                persistence.clearResponses();
                // =========== Brian - Added this for consistency
                FollowerState follower = new FollowerState();
                switchState(follower);
                return follower.handleVoteRequest(candidateTerm, candidateID, lastLogIndex, lastLogTerm);
            }
            return new VoteResult(persistence.getCurrentTerm(), false);
        }
    }

    // @param leader’s term
    // @param current leader
    // @param index of log entry before entries to append
    // @param term of log entry before entries to append
    // @param entries to append (in order of 0 to append.length-1)
    // @param index of highest committed entry
    // @return 0, if server appended entries; otherwise, server's
    // current term

    // Think this is done!!!!!!!
    public AppendResult handleAppendEntriesRequest(int leaderTerm, int leaderID, int prevLogIndex, int prevLogTerm, Entry[] entries,
                                                   int leaderCommit) {
        synchronized (raftStateLock) {


            int term = persistence.getCurrentTerm();


            // TODO: Check if this is greater than or equal to
            if (leaderTerm > term) {
                persistence.setCurrentTerm(leaderTerm, Optional.empty());
                myCurrentTimer.cancel();
                persistence.clearResponses();
                // ============= Brian - For consistency
                FollowerState follower = new FollowerState();
                switchState(follower);
                return follower.handleAppendEntriesRequest(leaderTerm, leaderID, prevLogIndex, prevLogTerm, entries, leaderCommit);
            }

            return failureAppend();
        }
    }

    // @param id of the timer that timed out

    // Think this is done !!!!!!!!!!
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

            testPrint("L: S" + selfRank + "." + term + "timeout, current entries: " + Arrays.toString(getEntries()) + " " +
					"resp: " + responses.toString());

            // ============ Brian - Moved this here to not clearAppendResponses right after sending them out
            myCurrentTimer = scheduleTimer(HEARTBEAT_INTERVAL, selfRank);
            persistence.clearResponses();
            testPrint("L: S" + selfRank + "." + term + "timeout, current entries: " + Arrays.toString(getEntries()) + " " +
					"resp: " + responses);

            for (int server = 1; server <= persistence.getServersNumber(); server++) {
				if (server == selfRank) {
					continue;
				}
                // TODO: Check this with TA
                // Brian - I added this to revert leader if it hears higher term RPC response
                if (responses.get(server) != null && responses.get(server).getTerm() > term) {
                    persistence.setCurrentTerm(responses.get(server).getTerm(), Optional.empty());
                    persistence.clearResponses();
                    switchState(new FollowerState());
                    return;
                }
//				else if (myResponses[server] > 0) {
//					nextIndex[server]--;
//				}
//				else if (myResponses[server] == 0){
//					nextIndex[server] = mLog.getLastIndex() + 1;
//				}
                // TODO: Check this added it to make sure nextIndex is updated if successful
                nextIndex[server] = 0;
                int entryIter = 0;
                Entry[] newEntries = new Entry[raftLog.getLastIndex() + 1 - nextIndex[server]];
                for (int iter = nextIndex[server]; iter <= raftLog.getLastIndex(); iter++) {
                    newEntries[entryIter] = raftLog.getEntry(iter);
                    entryIter++;
                }

                // TODO: Check with TA but added the -1 to indicate the one before where they will be added following
				//  Fig 2
//				Entry lastEntry = mLog.getEntry(nextIndex[server] - 1);

                testPrint("L: S" + selfRank + "." + term + "timeout, index of last entry" + (nextIndex[server] - 1));
                // TODO: lastEntry is sometimes null causing the following exception
                //
                //				Exception in thread "Timer-5" java.lang.NullPointerException
                //				at edu.duke.raft.LeaderMode.handleTimeout(LeaderMode.java:134)
                //				at edu.duke.raft.RaftMode$1.run(RaftMode.java:66)
                //				at java.base/java.util.TimerThread.mainLoop(Timer.java:556)
                //				at java.base/java.util.TimerThread.run(Timer.java:506)

                int lastEntryTerm;
//				if (nextIndex[server] - 1 < 0){
                lastEntryTerm = 0;
//				}
//				else {
//					lastEntryTerm = lastEntry.term;
//				}

                remoteAppendEntries(server, persistence.getCurrentTerm(), selfRank, nextIndex[server] - 1, lastEntryTerm,
						newEntries,
                        selfCommitIndex);
            }
        }
    }

    private void testPrint(String s) {
		System.out.println(s);
    }

    private Entry[] getEntries() {
		if (raftLog.getLastIndex() == -1) {
			return new Entry[0];
		}

        Entry[] myEntries = new Entry[raftLog.getLastIndex() + 1];
        for (int i = 0; i <= raftLog.getLastIndex(); i++) {
            myEntries[i] = raftLog.getEntry(i);
        }
        return myEntries;
    }
}
