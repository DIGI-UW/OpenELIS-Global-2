package org.openelisglobal.logging.controller;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class LoggingController {

    @Autowired
    private InMemoryLogAppender logBuffer;

    @GetMapping(path = "/logging")
    public void changeLoggingLevel(@RequestParam(name = "logLevel", defaultValue = "I") String logLevel,
            @RequestParam(name = "logger", defaultValue = "org.openelisglobal") String logger,
            @RequestParam(name = "rootLogLevel", defaultValue = "I") String rootLogLevel) {
        Level log4jLogLevel;
        switch (logLevel) {
        case "A":
            log4jLogLevel = Level.ALL;
            break;
        case "I":
            log4jLogLevel = Level.INFO;
            break;
        case "T":
            log4jLogLevel = Level.TRACE;
            break;
        case "D":
            log4jLogLevel = Level.DEBUG;
            break;
        case "W":
            log4jLogLevel = Level.WARN;
            break;
        case "E":
            log4jLogLevel = Level.ERROR;
            break;
        case "F":
            log4jLogLevel = Level.FATAL;
            break;
        case "O":
            log4jLogLevel = Level.OFF;
            break;
        default:
            log4jLogLevel = Level.ERROR;
            break;
        }
        if ("root".equals(logger)) {
            Configurator.setRootLevel(log4jLogLevel);
        } else {
            Configurator.setLevel(logger, log4jLogLevel);
        }
    }

    @GetMapping(path = "/logging/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamLog() {
        SseEmitter emitter = new SseEmitter(30L * 60L * 1000L);

        Consumer<String> subscriber = line -> {
            try {
                emitter.send(SseEmitter.event().data(line, MediaType.TEXT_PLAIN));
            } catch (IOException | IllegalStateException e) {
                emitter.complete();
            }
        };

        for (String line : logBuffer.snapshot()) {
            try {
                emitter.send(SseEmitter.event().data(line, MediaType.TEXT_PLAIN));
            } catch (IOException e) {
                emitter.completeWithError(e);
                return ResponseEntity.ok().headers(sseHeaders()).body(emitter);
            }
        }

        logBuffer.subscribe(subscriber);
        emitter.onCompletion(() -> logBuffer.unsubscribe(subscriber));
        emitter.onTimeout(() -> logBuffer.unsubscribe(subscriber));
        emitter.onError(e -> logBuffer.unsubscribe(subscriber));

        return ResponseEntity.ok().headers(sseHeaders()).body(emitter);
    }

    private HttpHeaders sseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Accel-Buffering", "no");
        headers.add("Cache-Control", "no-cache");
        return headers;
    }

    @GetMapping(path = "/logging/test")
    public void loggingLevelTest() {
        org.openelisglobal.common.log.LogEvent.logTrace(this.getClass().getSimpleName(), "changeLoggingLevel",
                "test logging message");
        org.openelisglobal.common.log.LogEvent.logDebug(this.getClass().getSimpleName(), "changeLoggingLevel",
                "test logging message");
        org.openelisglobal.common.log.LogEvent.logInfo(this.getClass().getSimpleName(), "changeLoggingLevel",
                "test logging message");
        org.openelisglobal.common.log.LogEvent.logWarn(this.getClass().getSimpleName(), "changeLoggingLevel",
                "test logging message");
        org.openelisglobal.common.log.LogEvent.logError(this.getClass().getSimpleName(), "changeLoggingLevel",
                "test logging message");
        org.openelisglobal.common.log.LogEvent.logFatal(this.getClass().getSimpleName(), "changeLoggingLevel",
                "test logging message");
    }

    @Component
    public static class InMemoryLogAppender extends AbstractAppender {

        private static final int BUFFER_CAPACITY = 300;
        private static final int DISPATCH_QUEUE_CAPACITY = 512;
        private static final String PATTERN = "%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ} -- %p -- %m";

        private final Deque<String> buffer = new ArrayDeque<>(BUFFER_CAPACITY);
        private final Set<Consumer<String>> subscribers = new CopyOnWriteArraySet<>();

        private final ThreadPoolExecutor dispatcher = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(DISPATCH_QUEUE_CAPACITY), new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "inmem-log-dispatcher");
                        t.setDaemon(true);
                        return t;
                    }
                }, new ThreadPoolExecutor.DiscardOldestPolicy());

        public InMemoryLogAppender() {
            super("InMemoryLogAppender", null, PatternLayout.newBuilder().withPattern(PATTERN)
                    .withCharset(java.nio.charset.StandardCharsets.UTF_8).build(), false, Property.EMPTY_ARRAY);
        }

        @PostConstruct
        @SuppressWarnings("resource")
        public void install() {
            start();
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration cfg = ctx.getConfiguration();
            cfg.addAppender(this);
            cfg.getRootLogger().addAppender(this, null, null);
            for (LoggerConfig lc : cfg.getLoggers().values()) {
                if (lc != cfg.getRootLogger() && !lc.isAdditive()) {
                    lc.addAppender(this, null, null);
                }
            }
            ctx.updateLoggers();
        }

        @Override
        public void append(LogEvent event) {
            String formatted;
            try {
                formatted = new String(getLayout().toByteArray(event), StandardCharsets.UTF_8).trim();
            } catch (Exception e) {
                return;
            }
            synchronized (buffer) {
                if (buffer.size() == BUFFER_CAPACITY) {
                    buffer.removeFirst();
                }
                buffer.addLast(formatted);
            }
            if (subscribers.isEmpty()) {
                return;
            }
            try {
                dispatcher.execute(() -> {
                    for (Consumer<String> sub : subscribers) {
                        try {
                            sub.accept(formatted);
                        } catch (Throwable ignored) {
                        }
                    }
                });
            } catch (RejectedExecutionException ignored) {
            }
        }

        @PreDestroy
        public void shutdown() {
            dispatcher.shutdownNow();
            stop();
        }

        public List<String> snapshot() {
            synchronized (buffer) {
                return new ArrayList<>(buffer);
            }
        }

        public void subscribe(Consumer<String> sub) {
            subscribers.add(sub);
        }

        public void unsubscribe(Consumer<String> sub) {
            subscribers.remove(sub);
        }
    }
}
