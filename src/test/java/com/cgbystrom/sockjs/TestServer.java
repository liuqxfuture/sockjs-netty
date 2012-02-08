package com.cgbystrom.sockjs;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import static org.jboss.netty.channel.Channels.pipeline;

public class TestServer {
    public static void main(String[] args) {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        LoggerContext loggerContext = rootLogger.getLoggerContext();
        loggerContext.reset();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%-5level %-20class{0}: %message%n");
        encoder.start();

        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<ILoggingEvent>();
        appender.setContext(loggerContext);
        appender.setEncoder(encoder);
        appender.start();

        rootLogger.addAppender(appender);

        //rootLogger.debug("Message 1");
        //rootLogger.warn("Message 2");

        InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());


        ServerBootstrap bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        final ServiceRouter router = new ServiceRouter();
        //new EchoService("/echo", CLIENT_URL, true)
        //EchoService echoService = ;
        router.registerService("/echo", new EchoService());
        router.registerService("/disabled_websocket_echo", new DisabledWebSocketEchoService());
        router.registerService("/close", new CloseService());

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = pipeline();
                pipeline.addLast("decoder", new HttpRequestDecoder());
                //pipeline.addLast("chunkAggregator", new HttpChunkAggregator(130 * 1024)); // Required for WS handshaker or else NPE.
                pipeline.addLast("encoder", new HttpResponseEncoder());
                pipeline.addLast("preflight", new PreflightHandler());
                //pipeline.addLast("handler", new RouterHandler(new IframeHandler()));
                //pipeline.addLast("iframe", new IframeHandler(CLIENT_URL));
                //pipeline.addLast("xhr-send", new XhrSendTransport());


                pipeline.addLast("router", router);
                //pipeline.addLast("sockjs-echo", new ServiceRouter("/echo", CLIENT_URL, true));
                //pipeline.addLast("sockjs-echo-disabled-ws", new ServiceRouter("/disabled_websocket_echo", CLIENT_URL, false));



                return pipeline;
            }
        });

        bootstrap.bind(new InetSocketAddress(8090));
        System.out.println("Server running..");
    }
}