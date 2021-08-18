// Copyright (C) 2020-2021 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.local.dao;

import com.nephest.battlenet.sc2.model.local.Evidence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class EvidenceDAO
{

    public static final int ACTIVE_MOD_THRESHOLD_DAYS = 14;

    public static final String STD_SELECT =
        "evidence.id AS \"evidence.id\", "
        + "evidence.player_character_report_id AS \"evidence.player_character_report_id\", "
        + "evidence.reporter_account_id AS \"evidence.reporter_account_id\", "
        + "evidence.reporter_ip AS \"evidence.reporter_ip\", "
        + "evidence.description AS \"evidence.description\", "
        + "evidence.status AS \"evidence.status\", "
        + "evidence.status_change_timestamp AS \"evidence.status_change_timestamp\", "
        + "evidence.created AS \"evidence.created\" ";

    public static final RowMapper<Evidence> STD_ROW_MAPPER = (rs, i)-> new Evidence
    (
        rs.getInt("evidence.id"),
        rs.getInt("evidence.player_character_report_id"),
        DAOUtils.getLong(rs, "evidence.reporter_account_id"),
        rs.getBytes("evidence.reporter_ip"),
        rs.getString("evidence.description"),
        DAOUtils.getBoolean(rs, "evidence.status"),
        rs.getObject("evidence.status_change_timestamp", OffsetDateTime.class),
        rs.getObject("evidence.created", OffsetDateTime.class)
    );

    public static final ResultSetExtractor<Evidence> STD_EXTRACTOR = (rs)->{
        if(!rs.next()) return null;
        return STD_ROW_MAPPER.mapRow(rs, 0);
    };

    private static final String CREATE_QUERY =
        "INSERT INTO evidence "
        + "(player_character_report_id, reporter_account_id, reporter_ip, description, status, "
            + "status_change_timestamp, created) "
        + "VALUES(:playerCharacterReportId, :reporterAccountId, :reporterIp, :description, :status, "
            + ":statusChangeTimestamp, :created)";
    private static final String GET_COUNT_BY_REPORTER_AND_TIMESTAMP_QUERY =
        "SELECT COUNT(*) "
        + "FROM evidence "
        + "WHERE (reporter_ip = :reporterIp OR reporter_account_id = :reporterAccountId) "
        + "AND created >= :from";
    private static final String GET_CONFIRMED_COUNT_BY_REPORT_QUERY =
        "SELECT COUNT(*) "
        + "FROM evidence "
        + "WHERE player_character_report_id = :playerCharacterReportId "
        + "AND status = true";
    private static final String GET_ALL_QUERY = "SELECT " + STD_SELECT + "FROM evidence ORDER BY created DESC";
    private static final String GET_BY_ID = "SELECT " + STD_SELECT + " FROM evidence WHERE id = :id";
    private static final String GET_BY_REPORT_IDS =
        "SELECT " + STD_SELECT
        + "FROM evidence "
        + "WHERE player_character_report_id IN (:reportIds) "
        + "ORDER BY created DESC";
    private static final String UPDATE_STATUS_QUERY =
        "WITH recent_evidence AS "
        + "("
            + "SELECT DISTINCT(evidence_id) "
            + "FROM evidence_vote "
            + "WHERE updated >= :from"
        + ") "
        + "UPDATE evidence "
        + "SET status = vote.vote, "
        + "status_change_timestamp = NOW() "
        + "FROM "
        + "("
            + "SELECT DISTINCT ON (evidence_id) "
            + "evidence_id, vote, COUNT(*) count "
            + "FROM recent_evidence "
            + "INNER JOIN evidence_vote USING(evidence_id)"
            + "GROUP BY evidence_id, vote "
            + "HAVING COUNT(*) >= :votesRequired "
            + "ORDER BY evidence_id DESC, count DESC, vote ASC"
        +") vote "
        + "WHERE id = vote.evidence_id "
        + "AND (status IS NULL OR status != vote.vote)";
    private static final String GET_ACTIVE_MOD_COUNT =
        "WITH last_reviewed AS  "
        + "( "
            + "SELECT voter_account_id, MAX(evidence_created) AS evidence_created, MAX(updated) AS updated "
            + "FROM evidence_vote "
            + "GROUP BY voter_account_id "
        + "), "
        + "last_not_reviewed AS "
        + "( "
            + "SELECT COUNT(*) AS count "
            + "FROM last_reviewed "
            + "LEFT JOIN LATERAL  "
            + "( "
                + "SELECT "
                + "voter_account_id, created "
                + "FROM evidence "
                + "WHERE created > last_reviewed.evidence_created "
                + "ORDER BY created ASC "
                + "LIMIT 1 "
            + ") last_not_reviewed_evidence ON true "

            + "WHERE GREATEST"
            + "("
                + "last_reviewed.evidence_created, "
                + "last_reviewed.updated,"
                + " COALESCE(last_not_reviewed_evidence.created, NOW())"
            + ") > NOW() - INTERVAL '" + ACTIVE_MOD_THRESHOLD_DAYS + " days' "
        + ") "
        + "SELECT last_not_reviewed.count "
        + "FROM last_not_reviewed";

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public EvidenceDAO(@Qualifier("sc2StatsNamedTemplate") NamedParameterJdbcTemplate template)
    {
        this.template = template;
    }

    public Evidence create(Evidence evidence)
    {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        MapSqlParameterSource params = createParams(evidence);
        template.update(CREATE_QUERY, params, keyHolder, new String[]{"id"});
        evidence.setId(keyHolder.getKey().intValue());
        return evidence;
    }

    public int getCount(byte[] ip, Long accountId, OffsetDateTime from)
    {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("reporterIp", ip)
            .addValue("reporterAccountId", accountId)
            .addValue("from", from);
        return template.query(GET_COUNT_BY_REPORTER_AND_TIMESTAMP_QUERY, params, DAOUtils.INT_EXTRACTOR);
    }

    public int getConfirmedCount(Integer reportId)
    {
        return template.query
        (
            GET_CONFIRMED_COUNT_BY_REPORT_QUERY,
            new MapSqlParameterSource().addValue("playerCharacterReportId", reportId),
            DAOUtils.INT_EXTRACTOR
        );
    }

    public List<Evidence> findAll()
    {
        return template.query(GET_ALL_QUERY, STD_ROW_MAPPER);
    }

    public Optional<Evidence> findById(int id)
    {
        return Optional.ofNullable(
            template.query(GET_BY_ID, new MapSqlParameterSource().addValue("id", id), STD_EXTRACTOR));
    }

    public List<Evidence> findByReportIds(Integer... reportIds)
    {
        if(reportIds.length == 0) return List.of();
        return template.query
        (
            GET_BY_REPORT_IDS,
            new MapSqlParameterSource().addValue("reportIds", Set.of(reportIds)),
            STD_ROW_MAPPER
        );
    }

    public int getActiveModCount()
    {
        return template.query(GET_ACTIVE_MOD_COUNT, DAOUtils.INT_EXTRACTOR);
    }

    @Cacheable(cacheNames = "evidence-required-votes")
    public int getRequiredVotes()
    {
        return Math.max((getActiveModCount() / 2) + 1, 2);
    }

    @CacheEvict
    (
        cacheNames={"evidence-required-votes"},
        allEntries=true
    )
    public void evictRequiredVotesCache(){}

    public int updateStatus(OffsetDateTime from)
    {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("from", from)
            .addValue("votesRequired", getRequiredVotes());
        return template.update(UPDATE_STATUS_QUERY, params);
    }

    private MapSqlParameterSource createParams(Evidence evidence)
    {
        return new MapSqlParameterSource()
            .addValue("playerCharacterReportId", evidence.getPlayerCharacterReportId())
            .addValue("reporterAccountId", evidence.getReporterAccountId())
            .addValue("reporterIp", evidence.getReporterIp())
            .addValue("description", evidence.getDescription())
            .addValue("status", evidence.getStatus())
            .addValue("statusChangeTimestamp", evidence.getStatusChangeDateTime())
            .addValue("created", evidence.getCreated());
    }

}