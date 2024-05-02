package ru.nsu.rpc.server;

import java.util.function.Function;

import com.alipay.remoting.AsyncContext;
import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.AbstractUserProcessor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RequestProcessor<T> extends AbstractUserProcessor<T> {
    private final Class<T> requestClass;
    private final Function<? super T, ?> requestHandler;

    @Override
    public void handleRequest(BizContext bizContext, AsyncContext asyncContext, T t) {
        throw new UnsupportedOperationException("bruh");
    }

    @Override
    public Object handleRequest(BizContext bizContext, T t) {
        return requestHandler.apply(t);
    }

    @Override
    public String interest() {
        return requestClass.getName();
    }
}
