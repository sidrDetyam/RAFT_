package ru.nsu.rpc;

import java.rmi.RemoteException;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.RpcServer;
import com.alipay.remoting.rpc.protocol.AbstractUserProcessor;
import ru.nsu.Entry;
import ru.nsu.statemachine.AbstractRaftState;
import ru.nsu.RaftServer;

public class RaftServerImpl implements RaftServer {
    private static RpcServer baseRpcServer;

    private static int mID;
    private static AbstractRaftState mMode;
    private static Object mLock;

    public RaftServerImpl(int serverID) {
        mID = serverID;
        baseRpcServer = new RpcServer(serverID, false, false);

        baseRpcServer.registerUserProcessor(new AbstractUserProcessor<RaftVotePayload>() {

            @Override
            public void handleRequest(BizContext bizContext, AsyncContext asyncContext, RaftVotePayload rpcPayload) {
                throw new UnsupportedOperationException(
                        "Raft Server not support handleRequest(BizContext bizCtx, AsyncContext asyncCtx, T request) ");
            }

            @Override
            public Object handleRequest(BizContext bizContext, RaftVotePayload rpcPayload) throws Exception {
                RaftVotePayload raftVotePayload = (RaftVotePayload) rpcPayload;
                return foo(requestVote(raftVotePayload.getCandidateTerm(), raftVotePayload.getCandidateID(),
                        raftVotePayload.getLastLogIndex(), raftVotePayload.getCandidateTerm()));
            }

            private Response foo(int resposne) {
                System.out.println("brugh %d".formatted(resposne));
                return new Response(resposne);
            }

            @Override
            public String interest() {
                return RaftVotePayload.class.getName();
            }
        });

        baseRpcServer.registerUserProcessor(new AbstractUserProcessor<RaftAppendPayload>() {

            @Override
            public void handleRequest(BizContext bizContext, AsyncContext asyncContext, RaftAppendPayload rpcPayload) {
                throw new UnsupportedOperationException(
                        "Raft Server not support handleRequest(BizContext bizCtx, AsyncContext asyncCtx, T request) ");
            }

            @Override
            public Object handleRequest(BizContext bizContext, RaftAppendPayload rpcPayload) throws Exception {
                RaftAppendPayload raftAppendPayload = (RaftAppendPayload) rpcPayload;
                return foo(appendEntries(raftAppendPayload.leaderTerm, raftAppendPayload.leaderID,
                        raftAppendPayload.prevLogIndex, raftAppendPayload.prevLogTerm,
                        raftAppendPayload.entries, raftAppendPayload.leaderCommit));
            }

            private Response foo(int resposne) {
                return new Response(resposne);
            }

            @Override
            public String interest() {
                return RaftAppendPayload.class.getName();
            }
        });

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
    public int requestVote(int candidateTerm,
                           int candidateID,
                           int lastLogIndex,
                           int lastLogTerm)
            throws RemoteException {
        if (mID == 1) {
            System.out.println("received");
        }

        synchronized (mLock) {
            var result = mMode.requestVote(candidateTerm,
                    candidateID,
                    lastLogIndex,
                    lastLogTerm);

            return result.voteGranted() ? 0 : result.term();
        }
    }

    // @return 0 if server appended entries under the leader's term;
    // otherwise, return server's current term
    public int appendEntries(int leaderTerm,
                             int leaderID,
                             int prevLogIndex,
                             int prevLogTerm,
                             Entry[] entries,
                             int leaderCommit)
            throws RemoteException {
        synchronized (mLock) {
            return mMode.appendEntries(leaderTerm,
                    leaderID,
                    prevLogIndex,
                    prevLogTerm,
                    entries,
                    leaderCommit);
        }
    }

    static {
        mLock = new Object();
    }
}
