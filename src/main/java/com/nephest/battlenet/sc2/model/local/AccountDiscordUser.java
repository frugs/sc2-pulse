// Copyright (C) 2020-2022 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.local;

import java.util.Objects;
import javax.validation.constraints.NotNull;

public class AccountDiscordUser
implements DiscordUserMeta
{

    @NotNull
    private Long accountId;

    @NotNull
    private Long discordUserId;

    private Boolean isPublic;

    public AccountDiscordUser()
    {
    }

    public AccountDiscordUser(Long accountId, Long discordUserId)
    {
        this.accountId = accountId;
        this.discordUserId = discordUserId;
    }

    public AccountDiscordUser(Long accountId, Long discordUserId, Boolean isPublic)
    {
        this.accountId = accountId;
        this.discordUserId = discordUserId;
        this.isPublic = isPublic;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {return true;}
        if (!(o instanceof AccountDiscordUser)) {return false;}
        AccountDiscordUser that = (AccountDiscordUser) o;
        return accountId.equals(that.accountId) && discordUserId.equals(that.discordUserId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(accountId, discordUserId);
    }

    @Override
    public String toString()
    {
        return "AccountDiscordUser[" + accountId + " " + discordUserId + ']';
    }

    public Long getAccountId()
    {
        return accountId;
    }

    public void setAccountId(Long accountId)
    {
        this.accountId = accountId;
    }

    public Long getDiscordUserId()
    {
        return discordUserId;
    }

    public void setDiscordUserId(Long discordUserId)
    {
        this.discordUserId = discordUserId;
    }

    @Override
    public Boolean isPublic()
    {
        return isPublic;
    }

    public void setPublic(Boolean aPublic)
    {
        isPublic = aPublic;
    }

}
