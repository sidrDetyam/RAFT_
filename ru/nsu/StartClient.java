package ru.nsu;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import ru.nsu.raftstate.dto.ClientCommandResult;
import ru.nsu.rpc.RpcException;
import ru.nsu.rpc.client.RaftRpcClientImpl;
import ru.nsu.rpc.dto.ClientRequestDto;
import ru.nsu.statemachine.StateMachineCommand;
import ru.nsu.statemachine.command.DeleteCommand;
import ru.nsu.statemachine.command.GetCommand;
import ru.nsu.statemachine.command.SetCommand;

public class StartClient {
    public static void main(String[] args_) {

        Map<String, Function<List<String>, StateMachineCommand>> commandsBuilders = Map.of(
                "s", args -> {
                    if (args.size() != 2) {
                        return null;
                    }
                    return new SetCommand(args.get(0), args.get(1));
                },
                "g", args -> {
                    if (args.size() != 1) {
                        return null;
                    }
                    return new GetCommand(args.get(0));
                },
                "d", args -> {
                    if (args.size() != 1) {
                        return null;
                    }
                    return new DeleteCommand(args.get(0));
                }
        );

        var client = new RaftRpcClientImpl();
        BiFunction<Integer, ClientRequestDto, ClientCommandResult> clientWrapper = (rank, request) -> {
            try {
                return client.clientRequest(rank, request);
            } catch (RpcException e) {
                throw new RuntimeException(e);
            }
        };

        BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            try {
                String line = buffer.readLine();
                var tokens = line.split(" ");
                if (tokens.length < 2) {
                    continue;
                }
                var rank = Integer.parseInt(tokens[0]);
                var commandName = tokens[1];
                var commandArgs = Arrays.stream(tokens).skip(2).toList();

                Optional.ofNullable(commandsBuilders.get(commandName))
                        .map(builder -> builder.apply(commandArgs))
                        .ifPresentOrElse(
                                command -> System.out.println(
                                        clientWrapper.apply(rank, new ClientRequestDto(command))
                                ),
                                () -> System.out.println("Invalid command")
                        );
            } catch (Throwable e) {
                System.out.println("E: " + e.getMessage());
            }
        }
    }
}
