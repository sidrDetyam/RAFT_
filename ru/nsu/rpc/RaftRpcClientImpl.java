package ru.nsu.rpc;

import java.util.Arrays;

import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import ru.nsu.Entry;
import ru.nsu.rpc.dto.AppendRequestDto;
import ru.nsu.rpc.dto.VoteRequestDto;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;

public class RaftRpcClientImpl {
    private static final int DEFAULT_TIMEOUT_MILLIS = 100;
    private final static RpcClient client = new RpcClient();

    static {
        client.startup();
    }

    public static VoteResult requestVote(int rank, VoteRequestDto voteRequestDto) throws RpcException {
        return invoke(rank,voteRequestDto, DEFAULT_TIMEOUT_MILLIS);
    }

    public static AppendResult appendEntries(int rank, AppendRequestDto appendRequestDto) throws RpcException {
        return invoke(rank, appendRequestDto, DEFAULT_TIMEOUT_MILLIS);
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
