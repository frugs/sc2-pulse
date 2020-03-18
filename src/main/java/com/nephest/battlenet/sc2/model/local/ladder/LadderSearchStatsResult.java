/*-
 * =========================LICENSE_START=========================
 * SC2 Ladder Generator
 * %%
 * Copyright (C) 2020 Oleksandr Masniuk
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * =========================LICENSE_END=========================
 */
package com.nephest.battlenet.sc2.model.local.ladder;

import java.util.Map;

import javax.validation.constraints.NotNull;

import com.nephest.battlenet.sc2.model.Race;

public class LadderSearchStatsResult
{

    @NotNull
    private final Long playerCount;

    @NotNull
    private final Long teamCount;

    @NotNull
    private final Map<Race, Long> gamesPlayed;

    public LadderSearchStatsResult
    (
        Long playerCount,
        Long teamCount,
        Map<Race, Long> gamesPlayed
    )
    {
        this.playerCount = playerCount;
        this.teamCount = teamCount;
        this.gamesPlayed = gamesPlayed;
    }

    public Long getPlayerCount()
    {
        return playerCount;
    }

    public Long getTeamCount()
    {
        return teamCount;
    }

    public Map<Race, Long> getGamesPlayed()
    {
        return gamesPlayed;
    }

}
