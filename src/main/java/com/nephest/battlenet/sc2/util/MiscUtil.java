// Copyright (C) 2020-2022 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.util;

import java.time.Duration;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MiscUtil
{

    private static final Logger LOG = LoggerFactory.getLogger(MiscUtil.class);

    public static void awaitAndLogExceptions(List<Future<?>> tasks, boolean clear)
    {
        for(Future<?> task : tasks)
        {
            try
            {
                task.get();
            }
            catch (ExecutionException | InterruptedException e)
            {
                LOG.error(e.getMessage(), e);
            }
        }
        if(clear) tasks.clear();
    }

    public static void awaitAndThrowException(List<Future<?>> tasks, boolean clear, boolean delayException)
    {
        Exception cause = null;

        for(Future<?> f : tasks)
        {
            try
            {
                f.get();
            }
            catch (InterruptedException | ExecutionException e)
            {
                if(!delayException) throw new IllegalStateException(e);

                cause = e;
                LOG.error(e.getMessage(), e);
            }
        }

        if(cause != null) throw new IllegalStateException(cause);
        if(clear) tasks.clear();
    }

    public static Duration sinceHourStart(Temporal temporal)
    {
        return Duration.between
        (
            temporal
                .with(ChronoField.MINUTE_OF_HOUR, 0)
                .with(ChronoField.SECOND_OF_MINUTE, 0)
                .with(ChronoField.NANO_OF_SECOND, 0),
            temporal
        );
    }

    public static Duration untilNextHour(Temporal temporal)
    {
        return Duration.ofHours(1).minus(sinceHourStart(temporal));
    }

    public static double getHourProgress(Temporal temporal)
    {
        return (sinceHourStart(temporal).toSeconds() / 3600.0);
    }

    public static int stringLength(int n)
    {
        if(n == Integer.MIN_VALUE) return 10;
        n = Math.abs(n);

        return n<100000?n<100?n<10?1:2:n<1000?3:n<10000?4:5:
            n<10000000?n<1000000?6:7:n<100000000?8: n<1000000000?9:10;
    }

}
