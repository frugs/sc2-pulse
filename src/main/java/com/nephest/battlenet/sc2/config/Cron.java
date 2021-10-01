// Copyright (C) 2020-2021 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.config;

import com.nephest.battlenet.sc2.model.BaseLeague;
import com.nephest.battlenet.sc2.model.QueueType;
import com.nephest.battlenet.sc2.model.Region;
import com.nephest.battlenet.sc2.model.local.InstantVar;
import com.nephest.battlenet.sc2.model.local.dao.*;
import com.nephest.battlenet.sc2.model.util.PostgreSQLUtils;
import com.nephest.battlenet.sc2.web.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Profile({"!maintenance & !dev"})
@Component
public class Cron
{

    private static final Logger LOG = LoggerFactory.getLogger(Cron.class);

    public static final Duration MATCH_UPDATE_FRAME = Duration.ofMinutes(50);
    public static final OffsetDateTime REPORT_UPDATE_FROM =
        OffsetDateTime.of(2021, 8, 17, 0, 0, 0, 0, ZoneOffset.UTC);
    public static final Duration MAINTENANCE_FREQUENT_FRAME = Duration.ofDays(2);
    public static final Duration MAINTENANCE_INFREQUENT_FRAME = Duration.ofDays(10);
    public static final Duration FORCED_LADDER_SCAN_FRAME = Duration.ofHours(2);
    public static final Duration MIN_UPDATE_FRAME = Duration.ofSeconds(210);

    private InstantVar heavyStatsInstant;
    private InstantVar maintenanceFrequentInstant;
    private InstantVar maintenanceInfrequentInstant;
    private InstantVar matchInstant;
    private InstantVar forcedLadderScanInstant;
    private UpdateContext matchUpdateContext;

    @Autowired
    private StatsService statsService;

    @Autowired
    private ProPlayerService proPlayerService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private SeasonStateDAO seasonStateDAO;

    @Autowired
    private SeasonDAO seasonDAO;

    @Autowired
    private TeamStateDAO teamStateDAO;

    @Autowired
    private PersistentLoginDAO persistentLoginDAO;

    @Autowired
    private PostgreSQLUtils postgreSQLUtils;

    @Autowired
    private ThreadPoolTaskScheduler executor;

    @Autowired
    private QueueStatsDAO queueStatsDAO;

    @Autowired
    private PlayerCharacterReportService characterReportService;

    @Autowired
    private VarDAO varDAO;

    @Autowired
    private VarService varService;

    @Autowired
    private UpdateService updateService;

    @PostConstruct
    public void init()
    {
        //catch exceptions to allow service autowiring for tests
        try {
            heavyStatsInstant = new InstantVar(varDAO, "ladder.stats.heavy.timestamp");
            forcedLadderScanInstant = new InstantVar(varDAO, "ladder.updated.forced");
            maintenanceFrequentInstant = new InstantVar(varDAO, "maintenance.frequent");
            maintenanceInfrequentInstant = new InstantVar(varDAO, "maintenance.infrequent");
            matchInstant = new InstantVar(varDAO, "match.updated");
        }
        catch(RuntimeException ex) {
            LOG.warn(ex.getMessage(), ex);
        }
    }

    public static OffsetDateTime getNextCharacterReportUpdateTime()
    {
        OffsetDateTime dt = OffsetDateTime.now().withHour(5).withMinute(0).withSecond(0).withNano(0);
        if(dt.compareTo(OffsetDateTime.now()) < 0) dt = dt.plusDays(1);
        return dt;
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 30_000)
    public void updateAll()
    {
        nonStopUpdate();
    }

    @Scheduled(cron="0 0 5 * * *")
    public void updateCharacterReports()
    {
        characterReportService.update(REPORT_UPDATE_FROM);
    }

    @Scheduled(cron="0 0 0/1 * * *")
    public void evictVarCache()
    {
        varService.evictCache();
    }

    @Scheduled(cron="0 59 * * * *")
    public void updateSeasonState()
    {
        seasonStateDAO.merge(OffsetDateTime.now(), seasonDAO.getMaxBattlenetId());
    }

    private void nonStopUpdate()
    {
        if(!shouldUpdate()) return;

        try
        {
            Instant begin = Instant.now();
            Instant lastMatchInstant = matchInstant.getValue();

            doUpdateSeasons();
            statsService.afterCurrentSeasonUpdate(updateService.getUpdateContext(null), false);
            calculateHeavyStats();
            updateService.updated(begin);
            if(!Objects.equals(lastMatchInstant, matchInstant.getValue())) matchUpdateContext =
                updateService.getUpdateContext(null);
            commenceMaintenance();
            LOG.info("Update cycle completed. Duration: {} seconds", (System.currentTimeMillis() - begin.toEpochMilli()) / 1000);
        }
        catch(RuntimeException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    private boolean calculateHeavyStats()
    {
        if(heavyStatsInstant.getValue() == null || System.currentTimeMillis() - heavyStatsInstant.getValue().toEpochMilli() >= 24 * 60 * 60 * 1000) {
            Instant defaultInstant = heavyStatsInstant.getValue() != null
                ? heavyStatsInstant.getValue()
                : Instant.now().minusSeconds(24 * 60 * 60 * 1000);
            OffsetDateTime defaultOdt = OffsetDateTime.ofInstant(defaultInstant, ZoneId.systemDefault());
            proPlayerService.update();
            queueStatsDAO.mergeCalculateForSeason(seasonDAO.getMaxBattlenetId());
            teamStateDAO.archive(defaultOdt);
            teamStateDAO.cleanArchive(defaultOdt);
            teamStateDAO.removeExpired();
            postgreSQLUtils.vacuum();
            postgreSQLUtils.analyze();
            heavyStatsInstant.setValueAndSave(Instant.ofEpochMilli(System.currentTimeMillis()));
            return true;
        }
        return false;
    }

    private boolean doUpdateSeasons(Region... regions)
    {
        boolean result = true;
        for(Region region : regions)
        {
            try
            {
                Instant begin = Instant.now();
                UpdateContext ctx = getLadderUpdateContext(region);
                if(ctx.getInternalUpdate() == null) LOG.info("Starting forced ladder scan: {}", region);
                statsService.updateCurrent
                (
                    new Region[]{region},
                    QueueType.getTypes(StatsService.VERSION).toArray(QueueType[]::new),
                    BaseLeague.LeagueType.values(),
                    false,
                    ctx
                );
                updateService.updated(region, begin);
                if(ctx.getInternalUpdate() == null) forcedLadderScanInstant.setValueAndSave(begin);
            }
            catch (RuntimeException ex)
            {
                //API can be broken randomly. All we can do at this point is log the exception.
                LOG.error(ex.getMessage(), ex);
                result = false;
            }
        }
        return result;
    }

    private void doUpdateSeasons()
    {
        List<Future<?>> tasks = new ArrayList<>();
        if(statsService.getAlternativeRegions().size() > 1)
        {
            tasks.add(executor.submit(()->doUpdateSeasons(Region.US, Region.CN)));
            tasks.add(executor.submit(()->doUpdateSeasons(Region.KR, Region.EU)));
        }
        else
        {
            for(Region region : Region.values()) tasks.add(executor.submit(()->doUpdateSeasons(region)));
        }

        Exception cause = null;
        for(Future<?> f : tasks)
        {
            try
            {
                f.get();
            }
            catch (InterruptedException | ExecutionException e)
            {
                cause = e;
            }
        }
        if(cause != null) throw new IllegalStateException(cause);

        try
        {
            if (shouldUpdateMatches())
            {
                matchService.update(matchUpdateContext == null ? updateService.getUpdateContext(null) : matchUpdateContext);
                matchInstant.setValueAndSave(Instant.now());
            }
        }
        catch (RuntimeException ex)
        {
            //API can be broken randomly. All we can do at this point is log the exception.
            LOG.error(ex.getMessage(), ex);
        }
    }

    private boolean shouldUpdateMatches()
    {
        return matchInstant.getValue() == null
            || System.currentTimeMillis() - matchInstant.getValue().toEpochMilli() >= MATCH_UPDATE_FRAME.toMillis();
    }

    private boolean shouldUpdate()
    {
        return updateService.getUpdateContext(null).getExternalUpdate() == null
            || System.currentTimeMillis() - updateService.getUpdateContext(null).getExternalUpdate().toEpochMilli()
                >= MIN_UPDATE_FRAME.toMillis();
    }

    private UpdateContext getLadderUpdateContext(Region region)
    {
        return forcedLadderScanInstant.getValue() == null
            || System.currentTimeMillis() - forcedLadderScanInstant.getValue().toEpochMilli() >= FORCED_LADDER_SCAN_FRAME.toMillis()
                ? new UpdateContext(null, null)
                : updateService.getUpdateContext(region);
    }

    private void commenceMaintenance()
    {
        if
        (
            maintenanceFrequentInstant.getValue() == null
            || System.currentTimeMillis() - maintenanceFrequentInstant.getValue().toEpochMilli() >= MAINTENANCE_FREQUENT_FRAME.toMillis()
        )
            commenceFrequentMaintenance();
        if
        (
            maintenanceInfrequentInstant.getValue() == null
            || System.currentTimeMillis() - maintenanceInfrequentInstant.getValue().toEpochMilli() >= MAINTENANCE_INFREQUENT_FRAME.toMillis()
        )
            commenceInfrequentMaintenance();
    }

    private void commenceFrequentMaintenance()
    {
        postgreSQLUtils.reindex("ix_match_updated");
        this.maintenanceFrequentInstant.setValueAndSave(Instant.now());
    }

    private void commenceInfrequentMaintenance()
    {
        postgreSQLUtils.reindex("ix_team_state_team_id_archived", "ix_team_state_timestamp");
        persistentLoginDAO.removeExpired();
        this.maintenanceInfrequentInstant.setValueAndSave(Instant.now());
    }

}
