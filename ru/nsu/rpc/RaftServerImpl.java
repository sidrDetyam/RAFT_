package ru.nsu.rpc;

import com.alipay.remoting.rpc.RpcServer;
import ru.nsu.RaftServer;
import ru.nsu.rpc.dto.AppendRequestDto;
import ru.nsu.rpc.dto.VoteRequestDto;
import ru.nsu.statemachine.AbstractRaftState;

public class RaftServerImpl implements RaftServer {
    private static RpcServer baseRpcServer;

    private static int mID;
    private static AbstractRaftState mMode;
    private static Object mLock;

    public RaftServerImpl(int serverID) {
        mID = serverID;
        baseRpcServer = new RpcServer(serverID, false, false);
        baseRpcServer.registerUserProcessor(new RequestProcessor<>(AppendRequestDto.class,
                this::handleAppendEntriesRequest));
        baseRpcServer.registerUserProcessor(new RequestProcessor<>(VoteRequestDto.class, this::handleVoteRequest));
        baseRpcServer.startup();
    }

    // @param the server's current mode
    public static void setMode(AbstractRaftState mode) {
        synchronized (mLock) {
            if (mode == null) {
                return;
            }
            // only change to a new mode
            if ((mMode == null) ||
                    (mMode.getClass() != mode.getClass())) {
                mMode = mode;
                mode.onSwitching();
            }
        }
    }

    // @param candidate’s term
    // @param candidate requesting vote
    // @param index of candidate’s last log entry
    // @param term of candidate’s last log entry
    // @return 0 if server votes for candidate under candidate's term;
    // otherwise, return server's current term
    @Override
    public int handleVoteRequest(VoteRequestDto request) {
        synchronized (mLock) {
            var result = mMode.handleVoteRequest(
                    request.getCandidateTerm(),
                    request.getCandidateID(),
                    request.getLastLogIndex(),
                    request.getLastLogTerm()
            );

            return result.voteGranted() ? 0 : result.term();
        }
    }

    // @return 0 if server appended entries under the leader's term;
    // otherwise, return server's current term
    @Override
    public int handleAppendEntriesRequest(AppendRequestDto request) {
        synchronized (mLock) {
            return mMode.handleAppendEntriesRequest(
                    request.getLeaderTerm(),
                    request.getLeaderID(),
                    request.getPrevLogIndex(),
                    request.getPrevLogTerm(),
                    request.getEntries(),
                    request.getLeaderCommit()
            );
        }
    }

    static {
        mLock = new Object();
    }
}
