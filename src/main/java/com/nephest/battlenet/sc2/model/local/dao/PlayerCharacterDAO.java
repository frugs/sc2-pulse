// Copyright (C) 2020 Oleksandr Masniuk and contributors
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.local.dao;

import com.nephest.battlenet.sc2.model.Region;
import com.nephest.battlenet.sc2.model.local.PlayerCharacter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PlayerCharacterDAO
{
    private static final String CREATE_QUERY = "INSERT INTO player_character "
        + "(account_id, region, battlenet_id, realm, name) "
        + "VALUES (:accountId, :region, :battlenetId, :realm, :name)";

    private static final String MERGE_QUERY = CREATE_QUERY
        + " "
        + "ON CONFLICT(region, battlenet_id) DO UPDATE SET "
        + "account_id=excluded.account_id, "
        + "realm=excluded.realm, "
        + "name=excluded.name";

    private static RowMapper<PlayerCharacter> STD_ROW_MAPPER;

    private final NamedParameterJdbcTemplate template;
    private final ConversionService conversionService;

    @Autowired
    public PlayerCharacterDAO
    (
        @Qualifier("sc2StatsNamedTemplate") NamedParameterJdbcTemplate template,
        @Qualifier("sc2StatsConversionService") ConversionService conversionService
    )
    {
        this.template = template;
        this.conversionService = conversionService;
        initMappers(conversionService);
    }

    private static void initMappers(ConversionService conversionService)
    {
        if(STD_ROW_MAPPER == null) STD_ROW_MAPPER = (rs, i)-> new PlayerCharacter
        (
            rs.getLong("player_character.id"),
            rs.getLong("player_character.account_id"),
            conversionService.convert(rs.getInt("player_character.region"), Region.class),
            rs.getLong("player_character.battlenet_id"),
            rs.getInt("player_character.realm"),
            rs.getString("player_character.name")
        );
    }

    public static RowMapper<PlayerCharacter> getStdRowMapper()
    {
        return STD_ROW_MAPPER;
    }

    public PlayerCharacter create(PlayerCharacter character)
    {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = createParameterSource(character);
        template.update(CREATE_QUERY, params, keyHolder, new String[]{"id"});
        character.setId(keyHolder.getKey().longValue());
        return character;
    }

    public PlayerCharacter merge(PlayerCharacter character)
    {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = createParameterSource(character);
        template.update(MERGE_QUERY, params, keyHolder, new String[]{"id"});
        character.setId(keyHolder.getKey().longValue());
        return character;
    }

    private MapSqlParameterSource createParameterSource(PlayerCharacter character)
    {
        return new MapSqlParameterSource()
            .addValue("accountId", character.getAccountId())
            .addValue("region", conversionService.convert(character.getRegion(), Integer.class))
            .addValue("battlenetId", character.getBattlenetId())
            .addValue("realm", character.getRealm())
            .addValue("name", character.getName());
    }

}
