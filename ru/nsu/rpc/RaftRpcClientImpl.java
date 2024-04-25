package ru.nsu.rpc;

import java.util.Arrays;

import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import ru.nsu.Entry;
import ru.nsu.rpc.dto.AppendRequestDto;
import ru.nsu.rpc.dto.VoteRequestDto;

public class RaftRpcClientImpl {
    private static final int DEFAULT_TIMEOUT_MILLIS = 100;
    private final static RpcClient client = new RpcClient();

    static {
        client.startup();
    }

    public static int requestVote(int rank,
                                  int candidateTerm,
                                  int candidateID,
                                  int lastLogIndex,
                                  int lastLogTerm) throws RpcException {
        return invoke(rank,
                new VoteRequestDto(candidateTerm, candidateID, lastLogIndex, lastLogTerm),
                DEFAULT_TIMEOUT_MILLIS);
    }

    public static int appendEntries(int rank,
                                    int leaderTerm,
                                    int leaderID,
                                    int prevLogIndex,
                                    int prevLogTerm,
                                    Entry[] entries,
                                    int leaderCommit) throws RpcException {
        Entry[] entriesCopy = Arrays.stream(entries).map(Entry::copy).toArray(Entry[]::new);
        return invoke(rank, new AppendRequestDto(leaderTerm, leaderID, prevLogIndex, prevLogTerm, entriesCopy,
                leaderCommit), DEFAULT_TIMEOUT_MILLIS);
    }

    private static <U> U invoke(int rank, Object request, int timeout) throws RpcException {
        try {
            return (U) client.invokeSync(targetUri(rank), request, timeout);
        } catch (RemotingException | InterruptedException e) {
            throw new RpcException("RPC exception", e);
        }
    }

    private static String targetUri(int rank) {
        return "localhost:%d".formatted(rank);
    }
}
