package ru.nsu.rpc;

import java.util.Arrays;

import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import ru.nsu.Entry;

public class RaftRpcClientImpl {

    private final static RpcClient client = new RpcClient();

    static {
        client.startup();
    }

    public static int requestVote(int rank,
                                  int candidateTerm,
                                  int candidateID,
                                  int lastLogIndex,
                                  int lastLogTerm) throws RpcException {
        return send(rank, new RaftVotePayload(candidateTerm, candidateID, lastLogIndex, lastLogTerm));
    }

    public static int appendEntries(int rank,
                                    int leaderTerm,
                                    int leaderID,
                                    int prevLogIndex,
                                    int prevLogTerm,
                                    Entry[] entries,
                                    int leaderCommit) throws RpcException {
        Entry[] entriesCopy = Arrays.stream(entries).map(Entry::copy).toArray(Entry[]::new);
        return send(rank, new RaftAppendPayload(leaderTerm, leaderID, prevLogIndex, prevLogTerm, entriesCopy,
                leaderCommit));
    }

    private static int send(int rank, RpcPayload rpcPayload) throws RpcException {
        try {
            return ((Response) client.invokeSync("localhost:%d".formatted(rank), rpcPayload, 10)).response;
        } catch (RemotingException | InterruptedException e) {
            throw new RpcException();
        }
    }
}
