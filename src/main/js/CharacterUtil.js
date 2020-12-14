// Copyright (C) 2020 Oleksandr Masniuk and contributors
// SPDX-License-Identifier: AGPL-3.0-or-later

class CharacterUtil
{

    static showCharacterInfo(e = null, explicitId = null)
    {
        if (e != null) e.preventDefault();
        const id = explicitId || e.currentTarget.getAttribute("data-character-id");

        const promises = [];
        const searchParams = new URLSearchParams();
        searchParams.append("type", "character");
        searchParams.append("id", id);
        const stringParams = searchParams.toString();
        searchParams.append("m", "1");
        promises.push(BootstrapUtil.hideActiveModal(["player-info", "error-generation"]));
        promises.push(CharacterUtil.updateCharacter(id));

        return Promise.all(promises)
            .then(o=>new Promise((res, rej)=>{
                if(!Session.isHistorical) HistoryUtil.pushState({type: "character", id: id}, document.title, "?" + searchParams.toString() + "#player-stats-summary");
                Session.currentSearchParams = stringParams;
                res();
            }))
            .then(e=>BootstrapUtil.showModal("player-info"));
    }

    static updateCharacterModel(id)
    {
        const request = "api/character/" + id + "/common";
        const characterPromise =
            fetch(request).then(resp => {if (!resp.ok) throw new Error(resp.statusText); return resp.json();})
        return Promise.all([characterPromise, StatsUtil.updateBundleModel()])
            .then(jsons => new Promise((res, rej)=>{
                const searchStd = jsons[0];
                searchStd.result = jsons[0].teams;
                Model.DATA.get(VIEW.CHARACTER).set(VIEW_DATA.SEARCH, searchStd);
                Model.DATA.get(VIEW.CHARACTER).set(VIEW_DATA.VAR, id);
                res(jsons);
             }));
    }

    static updateCharacterTeamsView()
    {
        const id = Model.DATA.get(VIEW.CHARACTER).get(VIEW_DATA.VAR);
        const searchResult = {result: Model.DATA.get(VIEW.CHARACTER).get(VIEW_DATA.SEARCH).teams};
        CharacterUtil.updateCharacterInfo(Model.DATA.get(VIEW.CHARACTER).get(VIEW_DATA.SEARCH), id);
        CharacterUtil.updateCharacterTeamsSection(searchResult);
    }

    static updateCharacter(id)
    {
        Util.setGeneratingStatus(STATUS.BEGIN);
        return CharacterUtil.updateCharacterModel(id)
            .then(jsons => new Promise((res, rej)=>{
                CharacterUtil.updateCharacterTeamsView();
                CharacterUtil.updateCharacterStatsView();
                CharacterUtil.updateCharacterLinkedCharactersView(id);
                Util.setGeneratingStatus(STATUS.SUCCESS);
                res();
            }))
            .catch(error => Util.setGeneratingStatus(STATUS.ERROR, error.message));
    }

    static updateCharacterInfo(commonCharacter, id)
    {
        const searchResult = commonCharacter.teams;
        const member = searchResult[0].members.filter(m=>m.character.id == id)[0];
        const account = member.account;
        const character = member.character;

        const info = document.getElementById("player-info");
        info.setAttribute("data-account-id", account.id);
        if(Session.currentAccount != null && Session.currentFollowing != null)
        {
            if(Object.values(Session.currentFollowing).filter(val=>val.followingAccountId == account.id).length > 0)
            {
                document.querySelector("#follow-button").classList.add("d-none");
                document.querySelector("#unfollow-button").classList.remove("d-none");
            }
            else
            {
                document.querySelector("#follow-button").classList.remove("d-none");
                document.querySelector("#unfollow-button").classList.add("d-none");
            }
        }

        CharacterUtil.updateCharacterInfoName(member);
        const region = EnumUtil.enumOfName(character.region, REGION);
        const profileLinkElement = document.getElementById("link-sc2");
        if(region == REGION.CN)
        {
            //the upstream site is not supporting the CN region.
            profileLinkElement.parentElement.classList.add("d-none");
        }
        else
        {
            const profileLink = `https://starcraft2.com/profile/${region.code}/${character.realm}/${character.battlenetId}`;
            profileLinkElement.setAttribute("href", profileLink);
            profileLinkElement.parentElement.classList.remove("d-none");
        }
        document.querySelector("#link-battletag span").textContent = account.battleTag;
        CharacterUtil.updateCharacterProInfo(commonCharacter);
    }

    static updateCharacterProInfo(commonCharacter)
    {
        for(const el of document.querySelectorAll(".pro-player-info")) el.classList.add("d-none");
        if(commonCharacter.proPlayer.proPlayer == null) return;

        for(const link of document.querySelectorAll("#revealed-report [rel~=nofollow]")) link.relList.remove("nofollow");
        const proPlayer = commonCharacter.proPlayer;
        document.querySelector("#pro-player-info").classList.remove("d-none");
        CharacterUtil.setProPlayerField("#pro-player-name", "td", proPlayer.proPlayer.name);
        CharacterUtil.setProPlayerField("#pro-player-birthday", "td", proPlayer.proPlayer.birthday != null
            ? Util.DATE_FORMAT.format(Util.parseIsoDate(proPlayer.proPlayer.birthday)) : null);
        CharacterUtil.setProPlayerField("#pro-player-country", "td", proPlayer.proPlayer.country);
        CharacterUtil.setProPlayerField("#pro-player-earnings", "td", proPlayer.proPlayer.earnings != null
            ? "$" + Util.NUMBER_FORMAT.format(proPlayer.proPlayer.earnings) : null);
        if(proPlayer.proTeam != null)
        {
            CharacterUtil.setProPlayerField("#pro-player-team", "td", proPlayer.proTeam.name);
        }
        for(const link of proPlayer.links)
        {
            const linkEl = document.querySelector("#link-" + link.type.toLowerCase());
            if(linkEl == null) continue;
            linkEl.setAttribute("href", link.url);
            linkEl.parentElement.classList.remove("d-none");
        }

    }

    static setProPlayerField(selector, sub, val)
    {
        if(val != null)
        {
            const nameEl = document.querySelector(selector);
            nameEl.querySelector(":scope " + sub).textContent = val;
            nameEl.classList.remove("d-none");
        }
    }

    static updateCharacterInfoName(member)
    {
        let charName;
        let charNameAdditional;
        const hashIx = member.character.name.indexOf("#");
        const nameNoHash = member.character.name.substring(0, hashIx);
        if(!Util.needToUnmaskName(nameNoHash, member.proNickname))
        {
            charName = nameNoHash;
            charNameAdditional = member.character.name.substring(hashIx);
        }
        else
        {
            charName = Util.unmaskName(member);
            charNameAdditional = `(${member.character.name})`;
        }
        document.getElementById("player-info-title-name").textContent = charName;
        const additionalNameElem = document.getElementById("player-info-title-name-additional");
        additionalNameElem.textContent = charNameAdditional;
        if(member.proNickname != null)
        {
            additionalNameElem.classList.add("player-pro");
        }
        else
        {
            additionalNameElem.classList.remove("player-pro");
        }
    }

    static updateCharacterTeamsSection(searchResultFull)
    {
        const searchResult = searchResultFull.result;
        grouped = searchResult.reduce(function(rv, x) {
            (rv[x["season"]] = rv[x["season"]] || []).push(x);
            return rv;
        }, {});

        const navs = document.querySelectorAll("#character-teams-section .nav-item");
        const panes = document.querySelectorAll("#character-teams-section .tab-pane");
        let shown = false;
        let ix = 0;

        for(const nav of navs) nav.classList.add("d-none");
        const groupedEntries = Object.entries(grouped);
        for(const [season, teams] of groupedEntries)
        {
            const nav = navs[ix];
            const link = nav.getElementsByClassName("nav-link")[0];
            const pane = panes[ix];
            const seasonFull = Session.currentSeasons.find(s=>s.battlenetId == season);
            const linkText = seasonFull.descriptiveName;
            link.textContent = linkText;
            if(!shown)
            {
                if(season == Session.currentSeason || ix == groupedEntries.length - 1)
                {
                    $(link).tab("show");
                    shown = true;
                }
            }
            const table = pane.getElementsByClassName("table")[0];
            TeamUtil.updateTeamsTable(table, {result: teams});
            nav.classList.remove("d-none");
            ix++;
        }
        ElementUtil.updateTabSelect(document.getElementById("teams-season-select"), navs);
    }

    static updateCharacterStatsView()
    {
        const searchResult = Model.DATA.get(VIEW.CHARACTER).get(VIEW_DATA.SEARCH).stats;
        for(const statsSection of document.getElementsByClassName("player-stats-dynamic")) statsSection.classList.add("d-none");
        for(const stats of searchResult)
        {
            const teamFormat = EnumUtil.enumOfId(stats.queueType, TEAM_FORMAT);
            const teamType = EnumUtil.enumOfId(stats.teamType, TEAM_TYPE);
            const raceName = stats.race == null ? "all" : EnumUtil.enumOfName(stats.race, RACE).name;
            const league = EnumUtil.enumOfId(stats.leagueMax, LEAGUE);
            const card = document.getElementById("player-stats-" + teamFormat.name + "-" + teamType.name);
            const raceStats = card.getElementsByClassName("player-stats-" + raceName)[0];
            raceStats.getElementsByClassName("player-stats-" + raceName + "-mmr")[0].textContent = stats.ratingMax;
            raceStats.getElementsByClassName("player-stats-" + raceName + "-games")[0].textContent = stats.gamesPlayed;
            const leagueStats = raceStats.getElementsByClassName("player-stats-" + raceName + "-league")[0];
            ElementUtil.removeChildren(leagueStats);
            leagueStats.appendChild(ElementUtil.createImage("league/", league.name, "table-image table-image-square"));
            raceStats.classList.remove("d-none");
            card.classList.remove("d-none");
        }
        for(const card of document.querySelectorAll(".player-stats-section:not(.d-none)"))
        {
            const table = card.querySelector(".player-stats-table");
            const visibleRows = table.querySelectorAll("tr.player-stats-dynamic:not(.d-none)");
            if
            (
                visibleRows.length === 2
                && visibleRows[0].querySelector(".player-stats-games").textContent
                    == visibleRows[1].querySelector(".player-stats-games").textContent
            )
                table.querySelector(".player-stats-all").classList.add("d-none");
            const gamesCol = table.querySelectorAll("th")[3];
            const mmrCol = table.querySelectorAll("th")[1];
            TableUtil.sortTable(table, [mmrCol, gamesCol]);
        }
    }

    static updateCharacterLinkedCharactersView(id)
    {
        const table = document.getElementById("linked-characters-table");
        for(const tr of table.querySelectorAll(":scope tr.active")) tr.classList.remove("active");
        const commonCharacter = Model.DATA.get(VIEW.CHARACTER).get(VIEW_DATA.SEARCH);
        CharacterUtil.updateCharacters(table, commonCharacter.linkedDistinctCharacters);
        table.querySelector(':scope a[data-character-id="' + id + '"]').closest("tr").classList.add("active");
    }

    static findCharactersByName()
    {
        CharacterUtil.updateCharacterSearch(document.getElementById("search-player-name").value);
    }

    static updateCharacterSearchModel(name)
    {
        const request = "api/characters?name=" + encodeURIComponent(name);
        return fetch(request)
            .then(resp => {if (!resp.ok) throw new Error(resp.statusText); return resp.json();})
            .then(json => new Promise((res, rej)=>{
                Model.DATA.get(VIEW.CHARACTER_SEARCH).set(VIEW_DATA.SEARCH, json);
                Model.DATA.get(VIEW.CHARACTER_SEARCH).set(VIEW_DATA.VAR, name);
                res(json);
            }));
    }

    static updateCharacterSearchView()
    {
        CharacterUtil.updateCharacters(document.getElementById("search-table"),  Model.DATA.get(VIEW.CHARACTER_SEARCH).get(VIEW_DATA.SEARCH));
        document.getElementById("search-result-all").classList.remove("d-none");
        Util.scrollIntoViewById("search-result-all");
    }

    static updateCharacters(table, searchResult)
    {
        const tbody = table.getElementsByTagName("tbody")[0];
        ElementUtil.removeChildren(tbody);

        for(let i = 0; i < searchResult.length; i++)
        {
            const character = searchResult[i];
            const row = tbody.insertRow();
            row.insertCell().appendChild(ElementUtil.createImage("flag/", character.members.character.region.toLowerCase(), "table-image-long"));
            row.insertCell().appendChild(ElementUtil.createImage("league/", EnumUtil.enumOfId(character.leagueMax, LEAGUE).name, "table-image table-image-square mr-1"));
            row.insertCell().textContent = character.ratingMax;
            row.insertCell().textContent = character.totalGamesPlayed;
            const membersCell = row.insertCell();
            membersCell.classList.add("complex", "cell-main");
            const mRow = document.createElement("span");
            mRow.classList.add("row", "no-gutters");
            const mInfo = TeamUtil.createMemberInfo(character, character.members);
            mInfo.getElementsByClassName("player-name")[0].classList.add("c-divider");
            const bTag = document.createElement("span");
            bTag.classList.add("c-divider", "battle-tag");
            bTag.textContent = character.members.account.battleTag;
            mInfo.getElementsByClassName("player-link-container")[0].appendChild(bTag);
            mRow.appendChild(mInfo);
            membersCell.appendChild(mRow);
            tbody.appendChild(row);
        }
    }

    static updateCharacterSearch(name)
    {
        Util.setGeneratingStatus(STATUS.BEGIN);
        const searchParams = new URLSearchParams();
        searchParams.append("type", "search");
        searchParams.append("name", name);
        const stringParams = searchParams.toString();
        return CharacterUtil.updateCharacterSearchModel(name)
            .then(json => new Promise((res, rej)=>{
                CharacterUtil.updateCharacterSearchView();
                Util.setGeneratingStatus(STATUS.SUCCESS);
                if(!Session.isHistorical) HistoryUtil.pushState({type: "search", name: name}, document.title, "?" + searchParams.toString() + "#search");
                Session.currentSearchParams = stringParams;
                res();
            }))
            .catch(error => Util.setGeneratingStatus(STATUS.ERROR, error.message));
    }

    static updatePersonalCharactersModel()
    {
        return fetch("api/my/characters")
            .then(resp => {if (!resp.ok) throw new Error(resp.status + " " + resp.statusText); return resp.json();})
            .then(json => new Promise((res, rej)=>{
                Model.DATA.get(VIEW.PERSONAL_CHARACTERS).set(VIEW_DATA.SEARCH, json);
                res(json);
            }));
    }

    static updatePersonalCharactersView()
    {
        CharacterUtil.updateCharacters(document.querySelector("#personal-characters-table"), Model.DATA.get(VIEW.PERSONAL_CHARACTERS).get(VIEW_DATA.SEARCH));
    }

    static updatePersonalCharacters()
    {
        Util.setGeneratingStatus(STATUS.BEGIN);
        return CharacterUtil.updatePersonalCharactersModel()
            .then(json => new Promise((res, rej)=>{
                CharacterUtil.updatePersonalCharactersView();
                Util.setGeneratingStatus(STATUS.SUCCESS);
                res();
            }))
            .catch(error => Util.setGeneratingStatus(STATUS.ERROR, error.message));
    }

    static enhanceSearchForm()
    {
        const form = document.getElementById("form-search");
        form.addEventListener("submit", function(evt)
            {
                evt.preventDefault();
                CharacterUtil.findCharactersByName();
            }
        );
    }

}