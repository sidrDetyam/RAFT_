package ru.nsu.rpc.client;

import com.alipay.remoting.exception.RemotingException;
import com.alipay.remoting.rpc.RpcClient;
import ru.nsu.raftstate.dto.ClientCommandResult;
import ru.nsu.rpc.RpcException;
import ru.nsu.rpc.dto.AppendRequestDto;
import ru.nsu.rpc.dto.VoteRequestDto;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.dto.ClientRequestDto;

public class RaftRpcClientImpl implements RaftRpcClient {
    private static final int DEFAULT_TIMEOUT_MILLIS = 100;
    private static final int CLIENT_TIMEOUT_MILLIS = 100000;
    private final RpcClient client = new RpcClient();

    public RaftRpcClientImpl() {
        client.startup();
    }

    @Override
    public VoteResult requestVote(int rank, VoteRequestDto voteRequestDto) throws RpcException {
        return invoke(rank,voteRequestDto, RaftRpcClientImpl.DEFAULT_TIMEOUT_MILLIS);
    }

    @Override
    public AppendResult appendEntries(int rank, AppendRequestDto appendRequestDto) throws RpcException {
        return invoke(rank, appendRequestDto, RaftRpcClientImpl.DEFAULT_TIMEOUT_MILLIS);
    }

    @Override
    public ClientCommandResult clientRequest(int rank, ClientRequestDto clientRequest) throws RpcException {
        return invoke(rank, clientRequest, CLIENT_TIMEOUT_MILLIS);
    }

    private <U> U invoke(int rank, Object request, int timeout) throws RpcException {
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
