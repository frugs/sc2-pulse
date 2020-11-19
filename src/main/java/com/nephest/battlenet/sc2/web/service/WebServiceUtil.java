// Copyright (C) 2020 Oleksandr Masniuk and contributors
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.web.service;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

import java.time.Duration;

public class WebServiceUtil
{

    private WebServiceUtil(){}

    public static final int RETRY_COUNT = 3;
    public static final Duration CONNECT_TIMEOUT = Duration.ofMillis(10000);
    public static final Duration IO_TIMEOUT = Duration.ofMillis(10000);
    public static final Duration RETRY_DURATION_MIN = Duration.ofMillis(300);
    public static final Duration RETRY_DURATION_MAX = Duration.ofMillis(1000);
    public static final Retry RETRY = Retry
        .backoff(RETRY_COUNT, RETRY_DURATION_MIN).maxBackoff(RETRY_DURATION_MAX)
        .filter(t->true)
        .transientErrors(true);

    public static WebClient.Builder getWebClientBuilder()
    {
        TcpClient timeoutClient = TcpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
            .doOnConnected
            (
                c-> c.addHandlerLast(new ReadTimeoutHandler((int) IO_TIMEOUT.toSeconds()))
                    .addHandlerLast(new WriteTimeoutHandler((int) IO_TIMEOUT.toSeconds()))
            );
        HttpClient httpClient = HttpClient.from(timeoutClient)
            .compress(true);
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

}