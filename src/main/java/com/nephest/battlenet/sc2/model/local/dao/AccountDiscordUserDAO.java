// Copyright (C) 2020-2022 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.local.dao;

import com.nephest.battlenet.sc2.model.local.AccountDiscordUser;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AccountDiscordUserDAO
{

    private static final String CREATE =
        "INSERT INTO account_discord_user(account_id, discord_user_id) "
        + "VALUES(:accountId, :discordUserId)";

    private static final String DELETE_BY_ACCOUNT_ID_OR_DISCORD_USER_ID =
        "DELETE FROM account_discord_user "
        + "WHERE account_id = :accountId "
        + "OR discord_user_id = :discordUserId";

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public AccountDiscordUserDAO
    (
        @Qualifier("sc2StatsNamedTemplate") NamedParameterJdbcTemplate template
    )
    {
        this.template = template;
    }

    public int[] create(AccountDiscordUser... users)
    {
        if(users.length == 0) return DAOUtils.EMPTY_INT_ARRAY;

        MapSqlParameterSource[] params = Arrays.stream(users)
            .filter(Objects::nonNull)
            .distinct()
            .map
            (
                u->new MapSqlParameterSource()
                    .addValue("accountId", u.getAccountId())
                    .addValue("discordUserId", u.getDiscordUserId())
            )
            .toArray(MapSqlParameterSource[]::new);
        return template.batchUpdate(CREATE, params);
    }

    public int remove(Long accountId, Long discordUserId)
    {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("accountId", accountId)
            .addValue("discordUserId", discordUserId);

        return template.update(DELETE_BY_ACCOUNT_ID_OR_DISCORD_USER_ID, params);
    }

}
