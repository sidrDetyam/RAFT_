package ru.nsu.rpc;

import ru.nsu.raftstate.dto.ClientCommandResult;
import ru.nsu.rpc.dto.AppendRequestDto;
import ru.nsu.rpc.dto.VoteRequestDto;
import ru.nsu.raftstate.AbstractRaftState;
import ru.nsu.raftstate.dto.AppendResult;
import ru.nsu.raftstate.dto.VoteResult;
import ru.nsu.rpc.dto.client.ClientRequest;

public class RpcServerImpl implements RpcServer {

    public RpcServerImpl(int serverID) {
        com.alipay.remoting.rpc.RpcServer baseRpcServer = new com.alipay.remoting.rpc.RpcServer(serverID, false, false);

        baseRpcServer.registerUserProcessor(new RequestProcessor<>(AppendRequestDto.class,
                this::handleAppendEntriesRequest));
        baseRpcServer.registerUserProcessor(new RequestProcessor<>(VoteRequestDto.class, this::handleVoteRequest));
        baseRpcServer.registerUserProcessor(new RequestProcessor<>(ClientRequest.class, this::handleClient));
        baseRpcServer.startup();
    }

    @Override
    public VoteResult handleVoteRequest(VoteRequestDto request) {
        return AbstractRaftState.executeStateSync(state -> state.handleVoteRequest(
                request.getCandidateTerm(),
                request.getCandidateID(),
                request.getLastLogIndex(),
                request.getLastLogTerm()
        ));
    }

    @Override
    public AppendResult handleAppendEntriesRequest(AppendRequestDto request) {
        return AbstractRaftState.executeStateSync(state -> state.handleAppendEntriesRequest(
                request.getLeaderTerm(),
                request.getLeaderID(),
                request.getPrevLogIndex(),
                request.getPrevLogTerm(),
                request.getEntries(),
                request.getLeaderCommit()
        ));
    }

    @Override
    public ClientCommandResult handleClient(ClientRequest clientRequest) {
        var cf = AbstractRaftState.executeStateSync(state -> state.handleClientCommand(clientRequest));
        return cf.join();
    }
}

