// Copyright (C) 2020 Oleksandr Masniuk and contributors
// SPDX-License-Identifier: AGPL-3.0-or-later

class SeasonUtil
{

    static seasonIdTranslator(id)
    {
        const season = Session.currentSeasons.filter((s)=>s.battlenetId == id)[0];
        const seasonEnd = season.end;
        const endDate = seasonEnd.getTime() - Date.now() > 0 ? new Date() : seasonEnd;
        return `${season.year} season ${season.number} (${season.battlenetId}) (${Util.MONTH_DATE_FORMAT.format(endDate)})`;
    }

    static getSeasons()
    {
        Util.setGeneratingStatus(STATUS.BEGIN);
        return Session.beforeRequest()
            .then(n=>fetch(ROOT_CONTEXT_PATH + "api/seasons"))
            .then(Session.verifyJsonResponse)
            .then(json => new Promise((res, rej)=>{SeasonUtil.updateSeasons(json); Util.setGeneratingStatus(STATUS.SUCCESS); res();}))
            .catch(error => Session.onPersonalException(error));
    }

    static updateSeasons(seasons)
    {
        Session.currentSeasons = seasons;
        Session.currentSeasonsMap = Util.groupBy(seasons, s=>s.battlenetId);
        for(const season of seasons) SeasonUtil.updateSeasonMeta(season);
        SeasonUtil.updateSeasonsTabs(seasons);
        for(const seasonPicker of document.querySelectorAll(".season-picker"))
        {
            ElementUtil.removeChildren(seasonPicker);
            for(const season of seasons)
            {
                const option = document.createElement("option");
                option.setAttribute("label", season.descriptiveName);
                option.textContent = season.descriptiveName;
                option.setAttribute("value", season.battlenetId);
                seasonPicker.appendChild(option);
            }
        }
    }

    static updateSeasonDuration(season)
    {
        season["days"] = (season.end - season.start) / (1000 * 60 * 60 * 24);
    }

    static updateSeasonDescription(season)
    {
        season.descriptiveName = SeasonUtil.seasonIdTranslator(season.battlenetId);
    }

    static updateSeasonDates(season)
    {
        const startDate = Util.parseIsoDate(season.start);
        let endDate = Util.parseIsoDate(season.end);
        const now = new Date();
        if(now - endDate < 0) endDate = now;
        season.start = startDate;
        season.end = endDate;
    }

    static updateSeasonMeta(season)
    {
        SeasonUtil.updateSeasonDates(season);
        SeasonUtil.updateSeasonDuration(season);
        SeasonUtil.updateSeasonDescription(season);
    }

    static updateSeasonsTabs(seasons)
    {
        const seasonPills = ElementUtil.createTabList(seasons.length, "character-teams-season", "4");
        seasonPills.nav.classList.add("d-none");
        const teamSection = document.getElementById("character-teams-section");
        BootstrapUtil.enhanceTabSelect(document.getElementById("teams-season-select"), seasonPills.nav);
        teamSection.appendChild(seasonPills.nav);
        for(const pane of seasonPills.pane.getElementsByClassName("tab-pane"))
        {
            const table = TableUtil.createTable(["Format", "Rank", "MMR", "League", "Region", "Team", "Games", "Win%", "Misc"]);
            table.querySelector("table").id = pane.id + "-table";
            const headers = table.querySelectorAll(":scope thead th");
            TableUtil.hoverableColumnHeader(headers[1]);
            TableUtil.hoverableColumnHeader(headers[6]);
            table.getElementsByTagName("table")[0].setAttribute("data-ladder-format-show", "true");
            pane.appendChild(table);
        }
        teamSection.appendChild(seasonPills.pane);
    }

    static updateSeasonState(searchParams)
    {
        searchParams.append("type", "online");
        const stringParams = searchParams.toString();
        const params = {params: stringParams};
        Util.setGeneratingStatus(STATUS.BEGIN);
        return SeasonUtil.updateSeasonStateModel(searchParams)
            .then(e=>new Promise((res, rej)=>{
                SeasonUtil.updateSeasonStateView(searchParams);
                Util.setGeneratingStatus(STATUS.SUCCESS);
                if(!Session.isHistorical) HistoryUtil.pushState(params, document.title, "?" + stringParams + "#online");
                Session.currentSearchParams = stringParams;
                if(!Session.isHistorical) HistoryUtil.updateActiveTabs();
                res();
            }))
            .catch(error => Session.onPersonalException(error));
    }

    static updateSeasonStateModel(searchParams)
    {
        const request = `${ROOT_CONTEXT_PATH}api/season/state/${searchParams.get("to")}/${searchParams.get("period")}`;
        return Session.beforeRequest()
            .then(n=>fetch(request))
            .then(Session.verifyJsonResponse)
            .then(json => new Promise((res, rej)=>{Model.DATA.get(VIEW.ONLINE).set(VIEW_DATA.SEARCH, json); res(json);}));
    }

    static updateSeasonStateView(searchParams)
    {
        SeasonUtil.updateSeasonStateViewPart(searchParams, "online-players-table", "playerCount");
        SeasonUtil.updateSeasonStateViewPart(searchParams, "online-games-table", "gamesPlayed");
        document.querySelector("#online-data").classList.remove("d-none");
    }

    static updateSeasonStateViewPart(searchParams, tableId, param)
    {
        const searchResult = Model.DATA.get(VIEW.ONLINE).get(VIEW_DATA.SEARCH);
        const data = {};
        for(const state of searchResult)
        {
            if(data[state.seasonState.periodStart] == null) data[state.seasonState.periodStart] = {};
            data[state.seasonState.periodStart][state.season.region] = state.seasonState[param] < 0 ? 0 : state.seasonState[param];
        }
        ChartUtil.CHART_RAW_DATA.set(tableId, {});
        TableUtil.updateVirtualColRowTable
        (
            document.getElementById(tableId),
            data,
            (tableData=>ChartUtil.CHART_RAW_DATA.get(tableId).data = tableData),
            (a, b)=>EnumUtil.enumOfName(a, REGION).order - EnumUtil.enumOfName(b, REGION).order,
            (name)=>EnumUtil.enumOfName(name, REGION).name,
            (dateTime)=>new Date(dateTime).getTime()
        );
    }

    static enhanceSeasonStateForm()
    {
        const form = document.querySelector("#form-online");
        document.querySelector("#online-to").valueAsNumber = Date.now();
        form.addEventListener("submit", evt=>{
            evt.preventDefault();
            const fd = new FormData(form);
            //the API expects exclusive "to", but user expects inclusive "to"
            const date = new Date(document.querySelector("#online-to").valueAsNumber);
            date.setHours(0, 0, 0, 0);
            date.setDate(date.getDate() + 1)
            fd.set("to", date.getTime());
            SeasonUtil.updateSeasonState(new URLSearchParams(Util.urlencodeFormData(fd)));
        });
    }

}
