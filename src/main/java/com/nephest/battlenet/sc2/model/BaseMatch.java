// Copyright (C) 2020 Oleksandr Masniuk and contributors
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public class BaseMatch
{

    public enum Decision
    implements Identifiable
    {

        WIN(1, "win"),
        LOSS(2, "loss"),
        TIE(3, "tie"),
        OBSERVER(4, "observer"),
        LEFT(5, "left"),
        DISAGREE(6, "disagree");

        private final int id;
        private final String name;

        Decision(int id, String name)
        {
            this.id = id;
            this.name = name;
        }

        public static Decision from(int id)
        {
            for(Decision decision : Decision.values())
            {
                if(decision.getId() == id) return decision;
            }
            throw new IllegalArgumentException("Invalid id: " + id);
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Decision from(String name)
        {
            for(Decision decision : Decision.values())
            {
                if(decision.getName().equalsIgnoreCase(name)) return decision;
            }
            throw new IllegalArgumentException("Invalid name: " + name);
        }

        @Override
        public int getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }

    }

    public enum MatchType
    implements Identifiable
    {
        
        _1V1(1, "1v1"),
        _2V2(2, "2v2"),
        _3V3(3, "3v3"),
        _4V4(4, "4v4"),
        ARCHON(5, "archon"),
        COOP(6, "coop", "(unknown)"),
        CUSTOM(7, "custom"),
        UNKNOWN(8, "");

        private final int id;
        private final String name;
        private final String additionalName;

        MatchType(int id, String name, String additionalName)
        {
            this.id = id;
            this.name = name;
            this.additionalName = additionalName;
        }

        MatchType(int id, String name)
        {
            this(id, name, null);
        }

        public static MatchType from(int id)
        {
            for(MatchType matchType : MatchType.values())
            {
                if(matchType.getId() == id) return matchType;
            }
            throw new IllegalArgumentException("Invalid id");
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static MatchType from(String name)
        {
            for(MatchType matchType : MatchType.values())
            {
                if(matchType.getName().equalsIgnoreCase(name)
                    || (matchType.getAdditionalName() != null && matchType.getAdditionalName().equalsIgnoreCase(name)))
                        return matchType;
            }
            return UNKNOWN;
        }

        @Override
        public int getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }

        public String getAdditionalName()
        {
            return additionalName;
        }

    }

    @NotNull
    private OffsetDateTime date;

    @NotNull
    private MatchType type;

    @NotNull
    private String map;

    public BaseMatch(){}

    public BaseMatch
    (
        @NotNull OffsetDateTime date, @NotNull MatchType type, @NotNull String map
    )
    {
        this.date = date;
        this.type = type;
        this.map = map;
    }

    public OffsetDateTime getDate()
    {
        return date;
    }

    public void setDate(OffsetDateTime date)
    {
        this.date = date;
    }

    public MatchType getType()
    {
        return type;
    }

    public void setType(MatchType type)
    {
        this.type = type;
    }

    public String getMap()
    {
        return map;
    }

    public void setMap(String map)
    {
        this.map = map;
    }

}
