package ru.nsu;

import ru.nsu.statemachine.command.GetCommand;
import ru.nsu.rpc.client.RaftRpcClientImpl;
import ru.nsu.rpc.RpcException;
import ru.nsu.rpc.dto.ClientRequestDto;

public class StartClient {
    public static void main(String[] args) throws RpcException {
        var client = new RaftRpcClientImpl();
        System.out.println(client.clientRequest(1, new ClientRequestDto(
//                new TestCommand(14)
//                new DeleteCommand("f")
//                new SetCommand("f", 42)
                new GetCommand("f")
        )));
    }
}
