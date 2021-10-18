// Copyright (C) 2020-2021 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.local.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nephest.battlenet.sc2.config.AllTestConfig;
import com.nephest.battlenet.sc2.model.*;
import com.nephest.battlenet.sc2.model.blizzard.BlizzardPlayerCharacter;
import com.nephest.battlenet.sc2.model.local.*;
import com.nephest.battlenet.sc2.model.local.inner.PlayerCharacterSummary;
import com.nephest.battlenet.sc2.web.controller.CharacterController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AllTestConfig.class)
@TestPropertySource("classpath:application.properties")
@TestPropertySource("classpath:application-private.properties")
public class PlayerCharacterSummaryIT
{

    @Autowired
    private DivisionDAO divisionDAO;

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private PlayerCharacterDAO playerCharacterDAO;

    @Autowired
    private TeamDAO teamDAO;

    @Autowired
    private TeamMemberDAO teamMemberDAO;

    @Autowired
    private TeamStateDAO teamStateDAO;

    @Autowired
    private SeasonGenerator seasonGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    private static MockMvc mvc;

    @BeforeEach
    public void beforeAll(@Autowired DataSource dataSource, @Autowired WebApplicationContext webApplicationContext)
    throws SQLException
    {
        try(Connection connection = dataSource.getConnection())
        {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema-drop-postgres.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema-postgres.sql"));
            mvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .alwaysDo(print())
                .build();
        }
    }

    @AfterEach
    public void afterAll(@Autowired DataSource dataSource)
    throws SQLException
    {
        try(Connection connection = dataSource.getConnection())
        {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema-drop-postgres.sql"));
        }
    }

    @Test
    public void testCornerCases()
    throws Exception
    {
        LocalDate now = LocalDate.now();
        LocalDate s3End = now;
        LocalDate s2End = now.minusDays(30);
        LocalDate s1End = s2End.minusDays(30);
        seasonGenerator.generateSeason
        (
            List.of
            (
                new Season(null, 3, Region.EU, 2021, 2, s3End.minusDays(30), s3End),
                new Season(null, 2, Region.EU, 2021, 1, s2End.minusDays(30), s2End),
                new Season(null, 1, Region.EU, 2020, 4, s1End.minusDays(30), s1End)
            ),
            List.of(BaseLeague.LeagueType.BRONZE, BaseLeague.LeagueType.SILVER),
            List.of(QueueType.LOTV_1V1, QueueType.LOTV_4V4),
            TeamType.ARRANGED,
            BaseLeagueTier.LeagueTierType.FIRST,
            0
        );

        Division division1v1_1 = divisionDAO
            .findDivision(1, Region.EU, QueueType.LOTV_1V1, TeamType.ARRANGED, 10).get();
        Division division1v1_2 = divisionDAO
            .findDivision(2, Region.EU, QueueType.LOTV_1V1, TeamType.ARRANGED, 10).get();
        Division division1v1_3 = divisionDAO
            .findDivision(3, Region.EU, QueueType.LOTV_1V1, TeamType.ARRANGED, 10).get();
        Account acc1 = accountDAO.merge(new Account(null, Partition.GLOBAL, "tag#1"));
        Account acc2 = accountDAO.merge(new Account(null, Partition.GLOBAL, "tag#2"));
        PlayerCharacter charEu1 = playerCharacterDAO.merge(new PlayerCharacter(null, acc1.getId(), Region.EU, 1L, 1, "name#1", null));
        PlayerCharacter charEu2 = playerCharacterDAO.merge(new PlayerCharacter(null, acc2.getId(), Region.EU, 2L, 2, "name#2", null));

        Team team1v1_p_s2 = teamDAO.merge(new Team(
            null, 2, Region.EU,
            new BaseLeague(BaseLeague.LeagueType.BRONZE, QueueType.LOTV_1V1, TeamType.ARRANGED),
            BaseLeagueTier.LeagueTierType.FIRST,
            teamDAO.legacyIdOf(new BlizzardPlayerCharacter[]{
                new BlizzardPlayerCharacter(charEu1.getBattlenetId(), charEu1.getRealm(), charEu1.getName())
            }, Race.PROTOSS),
            division1v1_2.getId(), 10L, 3, 3, 3, 1
        ))[0];

        Team team1v1_z_s3 = teamDAO.merge(new Team(
            null, 3, Region.EU,
            new BaseLeague(BaseLeague.LeagueType.BRONZE, QueueType.LOTV_1V1, TeamType.ARRANGED),
            BaseLeagueTier.LeagueTierType.FIRST,
            teamDAO.legacyIdOf(new BlizzardPlayerCharacter[]{
                new BlizzardPlayerCharacter(charEu1.getBattlenetId(), charEu1.getRealm(), charEu1.getName())
            }, Race.ZERG),
            division1v1_3.getId(), 10L, 3, 3, 3, 1
        ))[0];

        Team team1v1_t_s1 = teamDAO.merge(new Team(
            null, 1, Region.EU,
            new BaseLeague(BaseLeague.LeagueType.BRONZE, QueueType.LOTV_1V1, TeamType.ARRANGED),
            BaseLeagueTier.LeagueTierType.FIRST,
            teamDAO.legacyIdOf(new BlizzardPlayerCharacter[]{
                new BlizzardPlayerCharacter(charEu1.getBattlenetId(), charEu1.getRealm(), charEu1.getName())
            }, Race.TERRAN),
            division1v1_1.getId(), 10L, 3, 3, 3, 1
        ))[0];

        Team team1v1_t_s2 = teamDAO.merge(new Team(
            null, 2, Region.EU,
            new BaseLeague(BaseLeague.LeagueType.BRONZE, QueueType.LOTV_1V1, TeamType.ARRANGED),
            BaseLeagueTier.LeagueTierType.FIRST,
            teamDAO.legacyIdOf(new BlizzardPlayerCharacter[]{
                new BlizzardPlayerCharacter(charEu1.getBattlenetId(), charEu1.getRealm(), charEu1.getName())
            }, Race.TERRAN),
            division1v1_2.getId(), 10L, 3, 3, 3, 1
        ))[0];

        Team team1v1_t_s3 = teamDAO.merge(new Team(
            null, 3, Region.EU,
            new BaseLeague(BaseLeague.LeagueType.BRONZE, QueueType.LOTV_1V1, TeamType.ARRANGED),
            BaseLeagueTier.LeagueTierType.FIRST,
            teamDAO.legacyIdOf(new BlizzardPlayerCharacter[]{
                new BlizzardPlayerCharacter(charEu1.getBattlenetId(), charEu1.getRealm(), charEu1.getName())
            }, Race.TERRAN),
            division1v1_3.getId(), 10L, 3, 3, 3, 1
        ))[0];

        Team team1v1_2_z_s3 = teamDAO.merge(new Team(
            null, 3, Region.EU,
            new BaseLeague(BaseLeague.LeagueType.BRONZE, QueueType.LOTV_1V1, TeamType.ARRANGED),
            BaseLeagueTier.LeagueTierType.FIRST,
            teamDAO.legacyIdOf(new BlizzardPlayerCharacter[]{
                new BlizzardPlayerCharacter(charEu2.getBattlenetId(), charEu2.getRealm(), charEu2.getName())
            }, Race.ZERG),
            division1v1_3.getId(), 10L, 3, 3, 3, 1
        ))[0];

        teamMemberDAO.merge
        (
            new TeamMember(team1v1_p_s2.getId(), charEu1.getId(), 0, 1, 0, 0),
            new TeamMember(team1v1_z_s3.getId(), charEu1.getId(), 0, 0, 1, 0),
            new TeamMember(team1v1_t_s3.getId(), charEu1.getId(), 1, 0, 0, 0),
            new TeamMember(team1v1_t_s2.getId(), charEu1.getId(), 1, 0, 0, 0),
            new TeamMember(team1v1_t_s1.getId(), charEu1.getId(), 1, 0, 0, 0),
            new TeamMember(team1v1_2_z_s3.getId(), charEu2.getId(), 0, 0, 1, 0)
        );

        teamStateDAO.saveState
        (
            new TeamState(team1v1_t_s1.getId(), OffsetDateTime.now().minusDays(61), team1v1_t_s1.getDivisionId(), 8, 7),
            new TeamState(team1v1_p_s2.getId(), OffsetDateTime.now().minusDays(32), team1v1_p_s2.getDivisionId(), 8, 7),
            new TeamState(team1v1_t_s2.getId(), OffsetDateTime.now().minusDays(40), team1v1_t_s2.getDivisionId(), 3, 4),
            new TeamState(team1v1_t_s2.getId(), OffsetDateTime.now().minusDays(39), team1v1_t_s2.getDivisionId(), 4, 3),
            new TeamState(team1v1_t_s2.getId(), OffsetDateTime.now().minusDays(38), team1v1_t_s2.getDivisionId(), 3, 5),
            new TeamState(team1v1_t_s3.getId(), OffsetDateTime.now().minusDays(31), team1v1_t_s3.getDivisionId(), 3, 5)
        );

        PlayerCharacterSummary[] summary = objectMapper.readValue(mvc.perform
        (
            get("/api/character/{ids}/summary/1v1/50", charEu1.getId() + "," + charEu2.getId() + "," + charEu2.getId())
                .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString(), PlayerCharacterSummary[].class);

        assertEquals(4, summary.length);

        PlayerCharacterSummary c2z = Arrays.stream(summary)
            .filter(s-> s.getPlayerCharacterId().equals(charEu2.getId()) && s.getRace() == Race.ZERG)
            .findAny().orElseThrow();
        assertEquals(1, c2z.getGames());
        assertEquals(10, c2z.getRatingMax());
        assertEquals(10, c2z.getRatingAvg());

        PlayerCharacterSummary c1z = Arrays.stream(summary)
            .filter(s-> s.getPlayerCharacterId().equals(charEu1.getId()) && s.getRace() == Race.ZERG)
            .findAny().orElseThrow();
        assertEquals(1, c1z.getGames());
        assertEquals(10, c1z.getRatingMax());
        assertEquals(10, c1z.getRatingAvg());

        PlayerCharacterSummary c1p = Arrays.stream(summary)
            .filter(s-> s.getPlayerCharacterId().equals(charEu1.getId()) && s.getRace() == Race.PROTOSS)
            .findAny().orElseThrow();
        assertEquals(2, c1p.getGames());
        assertEquals(10, c1p.getRatingMax());
        assertEquals(9, c1p.getRatingAvg());

        PlayerCharacterSummary c1t = Arrays.stream(summary)
            .filter(s-> s.getPlayerCharacterId().equals(charEu1.getId()) && s.getRace() == Race.TERRAN)
            .findAny().orElseThrow();
        assertEquals(20, c1t.getGames());
        assertEquals(10, c1t.getRatingMax());
        assertEquals(6, c1t.getRatingAvg());
    }

    @Test
    public void testConstraints()
    throws Exception
    {
        mvc.perform
        (
            get("/api/character/1/summary/1v1/" + CharacterController.SUMMARY_DEPTH_MAX + 1)
                .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isBadRequest())
        //bugs out when parsing join, must be fixed in hte future
        //.andExpect(content().json("{\"message\":\"Depth is too big, max: " + CharacterController.SUMMARY_DEPTH_MAX +"\"}"))
        .andReturn();

        String longList = Stream.generate(()->"1")
            .limit(CharacterController.SUMMARY_IDS_MAX + 1)
            .collect(Collectors.joining(","));
        mvc.perform
        (
            get("/api/character/{ids}/summary/1v1/" + CharacterController.SUMMARY_DEPTH_MAX,  longList)
                .contentType(MediaType.APPLICATION_JSON)
        )
        .andExpect(status().isBadRequest())
        //bugs out when parsing join, must be fixed in hte future
        //.andExpect(content().json("{\"message\":\"Id list is too long, max: " + CharacterController.SUMMARY_IDS_MAX +"\"}"))
        .andReturn();

    }

}
