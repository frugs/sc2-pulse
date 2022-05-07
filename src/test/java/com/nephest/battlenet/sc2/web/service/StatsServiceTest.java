// Copyright (C) 2020-2022 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.web.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nephest.battlenet.sc2.model.BaseLeague;
import com.nephest.battlenet.sc2.model.QueueType;
import com.nephest.battlenet.sc2.model.TeamType;
import com.nephest.battlenet.sc2.model.blizzard.BlizzardTeam;
import com.nephest.battlenet.sc2.model.blizzard.BlizzardTeamMember;
import com.nephest.battlenet.sc2.model.local.Division;
import com.nephest.battlenet.sc2.model.local.League;
import com.nephest.battlenet.sc2.model.local.LeagueTier;
import com.nephest.battlenet.sc2.model.local.Season;
import com.nephest.battlenet.sc2.model.local.dao.SeasonDAO;
import com.nephest.battlenet.sc2.model.local.dao.TeamDAO;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.Validator;

public class StatsServiceTest
{

    private StatsService ss;

    private TeamDAO teamDAO;

    @BeforeEach
    public void beforeEach()
    {
        teamDAO = mock(TeamDAO.class);
        ss = new StatsService(null, null, mock(SeasonDAO.class), null, null, null, teamDAO, null, null, null,
            null, null, null, null, null, null, null, null, mock(Validator.class), null);
        StatsService nss = mock(StatsService.class);
        ss.setNestedService(nss);
    }

    @Test
    public void testInvalidTeam()
    {
        BlizzardTeam noMembersTeam = new BlizzardTeam();
        noMembersTeam.setWins(1);
        BlizzardTeam zeroGamesTeam = new BlizzardTeam();
        zeroGamesTeam.setMembers(new BlizzardTeamMember[]{new BlizzardTeamMember()});

        ss.updateTeams(new BlizzardTeam[]{noMembersTeam, zeroGamesTeam}, mock(Season.class),
            new League(1, 1, BaseLeague.LeagueType.BRONZE, QueueType.LOTV_1V1, TeamType.ARRANGED),
            mock(LeagueTier.class), mock(Division.class), Instant.now());

        verify(teamDAO, never()).merge(any());
    }
/*
    @Test
    public void testMemberTransaction()
    {
        BlizzardTierDivision[] bDivisions =
            {null, null, null, null, null, null, null, null, null, null, null, null};
        BlizzardLeagueTier bTier = new BlizzardLeagueTier();
        bTier.setDivisions(bDivisions);
        League league = new League();
        league.setQueueType(QueueType.LOTV_1V1);
        league.setTeamType(TeamType.ARRANGED);
        Season season = mock(Season.class);
        LeagueTier tier = mock(LeagueTier.class);

        ss.updateDivisions(bTier, season, league, tier);
        System.out.println(nss.toString());

        verify(nss).updateDivisions(bDivisions, season, league, tier, 0, 5);
        verify(nss).updateDivisions(bDivisions, season, league, tier, 5, 10);
        verify(nss).updateDivisions(bDivisions, season, league, tier, 10, 12);
        verifyNoMoreInteractions(nss);

        league.setQueueType(QueueType.LOTV_2V2);
        ss.updateDivisions(bTier, season, league, tier);
        verify(nss).updateDivisions(bDivisions, season, league, tier, 0, 2);
        verify(nss).updateDivisions(bDivisions, season, league, tier, 2, 4);
        verify(nss).updateDivisions(bDivisions, season, league, tier, 4, 6);
        verify(nss).updateDivisions(bDivisions, season, league, tier, 6, 8);
        verify(nss).updateDivisions(bDivisions, season, league, tier, 8, 10);
        //first invocation is from 1v1
        verify(nss, times(2)).updateDivisions(bDivisions, season, league, tier, 10, 12);
        verifyNoMoreInteractions(nss);

        bTier.setDivisions(new BlizzardTierDivision[0]);
        ss.updateDivisions(bTier, season, league, tier);
        verifyNoMoreInteractions(nss);
    }*/

}
