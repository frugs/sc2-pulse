// Copyright (C) 2020-2022 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.local.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nephest.battlenet.sc2.config.DatabaseTestConfig;
import com.nephest.battlenet.sc2.config.security.SC2PulseAuthority;
import com.nephest.battlenet.sc2.model.BaseLeague;
import com.nephest.battlenet.sc2.model.BaseLeagueTier;
import com.nephest.battlenet.sc2.model.BasePlayerCharacter;
import com.nephest.battlenet.sc2.model.Partition;
import com.nephest.battlenet.sc2.model.QueueType;
import com.nephest.battlenet.sc2.model.Region;
import com.nephest.battlenet.sc2.model.TeamType;
import com.nephest.battlenet.sc2.model.discord.DiscordUser;
import com.nephest.battlenet.sc2.model.discord.dao.DiscordUserDAO;
import com.nephest.battlenet.sc2.model.local.Account;
import com.nephest.battlenet.sc2.model.local.AccountDiscordUser;
import com.nephest.battlenet.sc2.model.local.AccountFollowing;
import com.nephest.battlenet.sc2.model.local.Clan;
import com.nephest.battlenet.sc2.model.local.Division;
import com.nephest.battlenet.sc2.model.local.Evidence;
import com.nephest.battlenet.sc2.model.local.EvidenceVote;
import com.nephest.battlenet.sc2.model.local.PlayerCharacter;
import com.nephest.battlenet.sc2.model.local.PlayerCharacterReport;
import com.nephest.battlenet.sc2.model.local.ProPlayer;
import com.nephest.battlenet.sc2.model.local.ProPlayerAccount;
import com.nephest.battlenet.sc2.model.local.SeasonGenerator;
import com.nephest.battlenet.sc2.model.local.Team;
import com.nephest.battlenet.sc2.model.local.TeamMember;
import com.nephest.battlenet.sc2.model.local.TeamState;
import com.nephest.battlenet.sc2.web.service.BlizzardPrivacyService;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

@SpringJUnitConfig(classes = DatabaseTestConfig.class)
@TestPropertySource("classpath:application.properties")
@TestPropertySource("classpath:application-private.properties")
public class PlayerCharacterDAOIT
{

    @Autowired
    private ProPlayerDAO proPlayerDAO;

    @Autowired
    private ProPlayerAccountDAO proPlayerAccountDAO;

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    private AccountRoleDAO accountRoleDAO;

    @Autowired
    private AccountFollowingDAO accountFollowingDAO;

    @Autowired
    private ClanDAO clanDAO;

    @Autowired
    private PlayerCharacterDAO playerCharacterDAO;

    @Autowired
    private DivisionDAO divisionDAO;

    @Autowired
    private TeamDAO teamDAO;

    @Autowired
    private TeamMemberDAO teamMemberDAO;

    @Autowired
    private TeamStateDAO teamStateDAO;

    @Autowired
    private PlayerCharacterReportDAO playerCharacterReportDAO;

    @Autowired
    private EvidenceDAO evidenceDAO;

    @Autowired
    private EvidenceVoteDAO evidenceVoteDAO;

    @Autowired
    private DiscordUserDAO discordUserDAO;

    @Autowired
    private AccountDiscordUserDAO accountDiscordUserDAO;

    @Autowired
    private SeasonGenerator seasonGenerator;

    @Autowired
    private JdbcTemplate template;

    @BeforeEach
    public void beforeEach(@Autowired DataSource dataSource)
    throws SQLException
    {
        try(Connection connection = dataSource.getConnection())
        {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema-drop-postgres.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema-postgres.sql"));
        }
    }

    @AfterEach
    public void afterEach(@Autowired DataSource dataSource)
    throws SQLException
    {
        try(Connection connection = dataSource.getConnection())
        {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema-drop-postgres.sql"));
        }
    }

    @Test
    public void testActiveCharacterFinder()
    {
        seasonGenerator.generateDefaultSeason
        (
            List.of(Region.EU),
            List.of(BaseLeague.LeagueType.BRONZE),
            List.of(QueueType.LOTV_2V2),
            TeamType.ARRANGED,
            BaseLeagueTier.LeagueTierType.FIRST,
            0
        );
        Division division = divisionDAO
            .findDivision(SeasonGenerator.DEFAULT_SEASON_ID, Region.EU, QueueType.LOTV_2V2, TeamType.ARRANGED, 10)
            .get();
        Account acc1 = accountDAO.merge(new Account(null, Partition.GLOBAL, "tag#1"));
        Account acc2 = accountDAO.merge(new Account(null, Partition.GLOBAL, "tag#2"));
        Account acc3 = accountDAO.merge(new Account(null, Partition.GLOBAL, "tag#3"));
        PlayerCharacter char1 = playerCharacterDAO.merge(new PlayerCharacter(null, acc1.getId(), Region.EU, 1L, 1, "name#1"));
        PlayerCharacter char2 = playerCharacterDAO.merge(new PlayerCharacter(null, acc2.getId(), Region.EU, 2L, 2, "name#2"));
        PlayerCharacter char3 = playerCharacterDAO.merge(new PlayerCharacter(null, acc3.getId(), Region.EU, 3L, 3, "name#3"));
        Team team1 = teamDAO.merge(new Team(
            null, SeasonGenerator.DEFAULT_SEASON_ID, Region.EU,
            new BaseLeague(BaseLeague.LeagueType.BRONZE, QueueType.LOTV_2V2, TeamType.ARRANGED),
            BaseLeagueTier.LeagueTierType.FIRST, new BigInteger("1"), division.getId(), 1L, 1, 1, 1, 1
        ))[0];
        Team team2 = teamDAO.merge(new Team(
            null, SeasonGenerator.DEFAULT_SEASON_ID, Region.EU,
            new BaseLeague(BaseLeague.LeagueType.BRONZE, QueueType.LOTV_2V2, TeamType.ARRANGED),
            BaseLeagueTier.LeagueTierType.FIRST, new BigInteger("2"), division.getId(), 2L, 2, 2, 2, 2
        ))[0];
        teamMemberDAO.merge
        (
            new TeamMember(team1.getId(), char1.getId(), 1, 0, 0, 0),
            new TeamMember(team1.getId(), char2.getId(), 0, 1, 0, 0),
            new TeamMember(team2.getId(), char1.getId(), 0, 0, 1, 0),
            new TeamMember(team2.getId(), char3.getId(), 0, 0, 0, 1)
        );
        OffsetDateTime now = OffsetDateTime.now();
        TeamState state1 = TeamState.of(team1);
        state1.setDateTime(now.minusHours(1));
        TeamState state2 = TeamState.of(team2);
        state2.setDateTime(now.minusHours(2));
        teamStateDAO.saveState(state1, state2);

        assertTrue(playerCharacterDAO.findRecentlyActiveCharacters(now.minusMinutes(59), Region.values()).isEmpty());

        List<PlayerCharacter> search1 = playerCharacterDAO.findRecentlyActiveCharacters(now.minusHours(1), Region.values());
        search1.sort(Comparator.comparing(PlayerCharacter::getId));
        assertEquals(2, search1.size());
        assertEquals(char1, search1.get(0));
        assertEquals(char2, search1.get(1));

        List<PlayerCharacter> search2 = playerCharacterDAO.findRecentlyActiveCharacters(now.minusHours(2), Region.values());
        search2.sort(Comparator.comparing(PlayerCharacter::getId));
        assertEquals(3, search2.size());
        assertEquals(char1, search2.get(0));
        assertEquals(char2, search2.get(1));
        assertEquals(char3, search2.get(2));

        List<PlayerCharacter> search3 = playerCharacterDAO
            .findTopRecentlyActiveCharacters(now.minusHours(2), QueueType.LOTV_2V2, TeamType.ARRANGED, List.of(Region.EU), 2);
        assertEquals(2, search3.size());
        search3.sort(Comparator.comparing(PlayerCharacter::getId));
        assertEquals(char1, search3.get(0));
        assertEquals(char3, search3.get(1));
    }

    @Test
    public void updateCharacters()
    {
        Account account = accountDAO.merge(new Account(null, Partition.GLOBAL, "tag#123"));
        Clan clan = clanDAO.merge(new Clan(null, "clanTag1", Region.EU, "clanName1"))[0];
        PlayerCharacter char1 = playerCharacterDAO
            .merge(new PlayerCharacter(null, account.getId(), Region.EU, 1L, 1, "name#123", clan.getId()));
        PlayerCharacter char2 = playerCharacterDAO
            .merge(new PlayerCharacter(null, account.getId(), Region.EU, 1L, 1, "name#123", clan.getId()));
        PlayerCharacter char3 = playerCharacterDAO
            .merge(new PlayerCharacter(null, account.getId(), Region.EU, 2L, 1, "name3#123", clan.getId()));

        PlayerCharacter[] updatedCharacters = new PlayerCharacter[]
        {
            new PlayerCharacter(null, account.getId(), Region.EU, 1L, 1, "name2#123"),
            new PlayerCharacter(null, account.getId(), Region.EU, 1L, 1, "name2#123"),
            new PlayerCharacter(null, account.getId(), Region.EU, 2L, 1, "name4#123", clan.getId())
        };

        OffsetDateTime minTimeAllowed = OffsetDateTime.now();
        assertEquals(2, playerCharacterDAO.updateCharacters(updatedCharacters));

        verifyUpdatedCharacters(minTimeAllowed);
    }

    private void verifyUpdatedCharacters(OffsetDateTime minTimeAllowed)
    {
        OffsetDateTime minTime = template.query("SELECT MIN(updated) FROM player_character", DAOUtils.OFFSET_DATE_TIME_RESULT_SET_EXTRACTOR);
        assertTrue(minTime.isAfter(minTimeAllowed));

        PlayerCharacter char1 = playerCharacterDAO.find(Region.EU, 1, 1L).orElseThrow();
        PlayerCharacter char2 = playerCharacterDAO.find(Region.EU, 1, 2L).orElseThrow();
        assertEquals("name2#123", char1.getName());
        assertNull(char1.getClanId());
        assertEquals("name4#123", char2.getName());
        assertEquals(1, char2.getClanId());

        template.execute
        (
            "UPDATE player_character "
            + "SET updated = NOW() - INTERVAL '" + (BlizzardPrivacyService.DATA_TTL.toDays() + 2) +  " days' "
            + "WHERE id = 1"
        );
        playerCharacterDAO.anonymizeExpiredCharacters(OffsetDateTime.now().minusSeconds(BlizzardPrivacyService.DATA_TTL.toSeconds()).minusDays(1));
        //character is excluded due to "from' param
        assertEquals("name2#123", playerCharacterDAO.find(Region.EU, 1, 1L).orElseThrow().getName());

        playerCharacterDAO.anonymizeExpiredCharacters(OffsetDateTime.MIN);
        assertEquals(BasePlayerCharacter.DEFAULT_FAKE_FULL_NAME, playerCharacterDAO.find(Region.EU, 1, 1L).orElseThrow().getName());
    }

    @Test
    public void updateAccountsAndCharacters()
    {
        Account acc1 = accountDAO.merge(new Account(null, Partition.GLOBAL, "tag#123"));
        Account acc2 = accountDAO.merge(new Account(null, Partition.GLOBAL, "tag2#123"));
        Account acc3 = accountDAO.merge(new Account(null, Partition.GLOBAL, "tag10#123"));
        Clan clan = clanDAO.merge(new Clan(null, "clanTag1", Region.EU, "clanName1"))[0];
        PlayerCharacter char1 = playerCharacterDAO
            .merge(new PlayerCharacter(null, acc1.getId(), Region.EU, 1L, 1, "name#123", clan.getId()));
        PlayerCharacter char2 = playerCharacterDAO
            .merge(new PlayerCharacter(null, acc1.getId(), Region.EU, 1L, 1, "name#123", clan.getId()));
        PlayerCharacter char3 = playerCharacterDAO
            .merge(new PlayerCharacter(null, acc2.getId(), Region.EU, 2L, 1, "name3#123", clan.getId()));
        PlayerCharacter char4 = playerCharacterDAO
            .merge(new PlayerCharacter(null, acc1.getId(), Region.EU, 3L, 1, "name10#123", clan.getId()));
        PlayerCharacter char5 = playerCharacterDAO
            .merge(new PlayerCharacter(null, acc1.getId(), Region.EU, 4L, 1, "name20#123", clan.getId()));

        List<Tuple3<Account, PlayerCharacter, Boolean>> updatedAccsAndChars = List.of
        (
            Tuples.of
            (
                new Account(null, Partition.GLOBAL, "tag3#123"),
                new PlayerCharacter(null, acc1.getId(), Region.EU, 1L, 1, "name2#123"),
                true
            ),
            Tuples.of
            (
                new Account(null, Partition.GLOBAL, "tag3#123"),
                new PlayerCharacter(null, acc1.getId(), Region.EU, 1L, 1, "name2#123"),
                true
            ),
            Tuples.of
            (
                new Account(null, Partition.GLOBAL, "tag4#123"),
                new PlayerCharacter(null, acc2.getId(), Region.EU, 2L, 1, "name4#123", clan.getId()),
                true
            ),
            Tuples.of
            (
                new Account(null, Partition.GLOBAL, "tag10#123"),
                new PlayerCharacter(null, acc1.getId(), Region.EU, 3L, 1, "name11#123"),
                false
            ),
            Tuples.of
            (
                new Account(null, Partition.GLOBAL, "tag10#123"),
                new PlayerCharacter(null, acc1.getId(), Region.EU, 4L, 1, "name20#1"),
                false
            )
        );

        OffsetDateTime char4Updated =
            template.query("SELECT updated FROM player_character WHERE id = " + char4.getId(), DAOUtils.OFFSET_DATE_TIME_RESULT_SET_EXTRACTOR);
        OffsetDateTime minTimeAllowed = OffsetDateTime.now();
        assertEquals(3, playerCharacterDAO.updateAccountsAndCharacters(updatedAccsAndChars));

        //character is not updated
        assertEquals("name10#123", playerCharacterDAO.find(Region.EU, 1, 3L).orElseThrow().getName());
        OffsetDateTime char4UpdatedNew =
            template.query("SELECT updated FROM player_character WHERE id = " + char4.getId(), DAOUtils.OFFSET_DATE_TIME_RESULT_SET_EXTRACTOR);
        assertTrue(char4UpdatedNew.isEqual(char4Updated));

        //char5 is updated despite being stale because it has the same name prefix
        OffsetDateTime char5Updated =
            template.query("SELECT updated FROM player_character WHERE id = " + char5.getId(), DAOUtils.OFFSET_DATE_TIME_RESULT_SET_EXTRACTOR);
        assertTrue(char5Updated.isAfter(minTimeAllowed));

        template.execute("UPDATE player_character SET updated = NOW() WHERE id = " + char4.getId());

        verifyUpdatedCharacters(minTimeAllowed);
        OffsetDateTime minAccTime =
            template.query("SELECT MIN(updated) FROM account", DAOUtils.OFFSET_DATE_TIME_RESULT_SET_EXTRACTOR);
        assertTrue(minAccTime.isAfter(minTimeAllowed));

        assertEquals("tag3#123", accountDAO.findByIds(acc1.getId()).get(0).getBattleTag());
        assertEquals("tag4#123", accountDAO.findByIds(acc2.getId()).get(0).getBattleTag());
        //character is rebound to another account
        assertEquals(acc3.getId(), playerCharacterDAO.find(Region.EU, 1, 3L).orElseThrow().getAccountId());

        template.execute
        (
            "UPDATE account "
            + "SET updated = NOW() - INTERVAL '" + (BlizzardPrivacyService.DATA_TTL.toDays() + 2) + " days' "
            + "WHERE id = " + acc1.getId()
        );
        accountDAO.anonymizeExpiredAccounts(OffsetDateTime.now().minusSeconds(BlizzardPrivacyService.DATA_TTL.toSeconds()).minusDays(1));
        //the account is excluded due to "from" param
        assertEquals("tag3#123", accountDAO.findByIds(acc1.getId()).get(0).getBattleTag());

        accountDAO.anonymizeExpiredAccounts(OffsetDateTime.MIN);
        assertEquals(BasePlayerCharacter.DEFAULT_FAKE_NAME + "#211", accountDAO.findByIds(acc1.getId()).get(0).getBattleTag());
    }

    @Test
    public void testCharacterRebinding()
    {
        //stub
        stubCharacterChain(1, true);
        stubCharacterChain(2, true);
        stubCharacterChain(3, true);
        stubCharacterChain(4, false);
        stubReport(1, 2, true);
        stubReport(3, 4, false);
        accountFollowingDAO.create(new AccountFollowing(1L, 1L));
        accountFollowingDAO.create(new AccountFollowing(2L, 2L));
        accountFollowingDAO.create(new AccountFollowing(3L, 3L));
        accountRoleDAO.addRoles(1, SC2PulseAuthority.ADMIN);
        accountRoleDAO.addRoles(2, SC2PulseAuthority.ADMIN, SC2PulseAuthority.MODERATOR);

        //rebind character2 to account 1
        playerCharacterDAO.merge(new PlayerCharacter(null, 1L, Region.EU, 2L, 2, "name2"));

        //rebound value already exists, do nothing, it will be cleared later when account expires
        List<ProPlayerAccount> proPlayerAccounts = proPlayerAccountDAO.findByProPlayerId(2);
        assertEquals(1, proPlayerAccounts.size());
        assertEquals(2, proPlayerAccounts.get(0).getAccountId());

        List<AccountFollowing> followings = accountFollowingDAO.findAccountFollowingList(2L);
        assertEquals(1, followings.size());
        assertEquals(2L, followings.get(0).getAccountId());
        assertEquals(2L, followings.get(0).getFollowingAccountId());

        evidenceVoteDAO.findByEvidenceIds(1).stream()
            .filter(v->v.getVoterAccountId() == 2)
            .findAny()
            .orElseThrow();

        DiscordUser discordUser2 = discordUserDAO.findByAccountId(2L, false).orElseThrow();
        assertEquals(2L, discordUser2.getId());

        //admin is untouched because it already exists, moderator is rebound
        List<SC2PulseAuthority> roles = accountRoleDAO.getRoles(1);
        assertEquals(3, roles.size());
        assertTrue(roles.contains(SC2PulseAuthority.ADMIN));
        assertTrue(roles.contains(SC2PulseAuthority.MODERATOR));

        List<SC2PulseAuthority> roles2 = accountRoleDAO.getRoles(2);
        assertEquals(2, roles2.size());
        assertTrue(roles2.contains(SC2PulseAuthority.ADMIN));

        //rebind character 3 to account 4
        playerCharacterDAO.merge(new PlayerCharacter(null, 4L, Region.EU, 3L, 3, "name3"));

        //rebound value does not exist, successfully rebound
        List<ProPlayerAccount> proPlayerAccountsRebound = proPlayerAccountDAO.findByProPlayerId(3);
        assertEquals(1, proPlayerAccountsRebound.size());
        assertEquals(4, proPlayerAccountsRebound.get(0).getAccountId());

        //old followings were moved to the new account
        List<AccountFollowing> followingsRebound = accountFollowingDAO.findAccountFollowingList(3L);
        assertTrue(followingsRebound.isEmpty());

        List<AccountFollowing> followingsRebound2 = accountFollowingDAO.findAccountFollowingList(4L);
        assertEquals(1, followingsRebound2.size());
        assertEquals(4L, followingsRebound2.get(0).getAccountId());
        assertEquals(4L, followingsRebound2.get(0).getFollowingAccountId());

        assertEquals(4, evidenceDAO.findById(false, 3).orElseThrow().getReporterAccountId());

        List<EvidenceVote> reboundVotes = evidenceVoteDAO.findByEvidenceIds(3);
        assertEquals(1, reboundVotes.size());
        assertEquals(4L, reboundVotes.get(0).getVoterAccountId());

        assertFalse(discordUserDAO.findByAccountId(3L, false).isPresent());
        DiscordUser discordUser3 = discordUserDAO.findByAccountId(4L, false).orElseThrow();
        assertEquals(3L, discordUser3.getId());
    }

    private Account stubCharacterChain(int num, boolean bind)
    {
        DiscordUser discordUser = discordUserDAO
            .merge(new DiscordUser((long) num, "name" + num, num))[0];
        ProPlayer proPlayer = proPlayerDAO
            .merge(new ProPlayer(null, new byte[(byte) num], "proTag" + num, "proName" + num));
        Account acc1 = accountDAO.merge(new Account(null, Partition.GLOBAL, "tag" + num));
        PlayerCharacter char1 = playerCharacterDAO
            .merge(new PlayerCharacter(null, acc1.getId(), Region.EU, (long) num, num, "name" + num));
        if(bind)
        {
            proPlayerAccountDAO.link(proPlayer.getId(), "tag" + num);
            accountDiscordUserDAO.create(new AccountDiscordUser(acc1.getId(), discordUser.getId()));
        }
        return acc1;
    }

    private void stubReport(long accountId, long accountId2, boolean secondVote)
    {
        PlayerCharacterReport report = playerCharacterReportDAO.merge
        (
            new PlayerCharacterReport
            (
                null,
                accountId,
                null,
                PlayerCharacterReport.PlayerCharacterReportType.CHEATER,
                false,
                OffsetDateTime.now()
            )
        );
        Evidence evidence = evidenceDAO.create
        (
            new Evidence
            (
                null,
                report.getId(),
                accountId,
                null,
                "description",
                false,
                OffsetDateTime.now(), OffsetDateTime.now()
            )
        );
        evidenceVoteDAO.merge
        (
            new EvidenceVote
            (
                evidence.getId(),
                OffsetDateTime.now(),
                accountId,
                true,
                OffsetDateTime.now()
            )
        );
        if(secondVote) evidenceVoteDAO.merge
        (
            new EvidenceVote
            (
                evidence.getId(),
                OffsetDateTime.now(),
                accountId2,
                true,
                OffsetDateTime.now()
            )
        );
        Evidence evidence2 = evidenceDAO.create
        (
            new Evidence
            (
                null,
                report.getId(),
                accountId2,
                null,
                "description",
                false,
                OffsetDateTime.now(), OffsetDateTime.now()
            )
        );
    }

}
