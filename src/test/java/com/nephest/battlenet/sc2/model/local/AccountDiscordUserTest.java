// Copyright (C) 2020-2022 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.local;

import com.nephest.battlenet.sc2.util.TestUtil;
import org.junit.jupiter.api.Test;

public class AccountDiscordUserTest
{

    @Test
    public void testUniqueness()
    {
        AccountDiscordUser user = new AccountDiscordUser(1L, 1L);
        AccountDiscordUser equalUser = new AccountDiscordUser(1L, 1L);
        AccountDiscordUser[] notEqualUsers = new AccountDiscordUser[]
        {
            new AccountDiscordUser(2L, 1L),
            new AccountDiscordUser(1L, 2L)
        };

        TestUtil.testUniqueness(user, equalUser, (Object[]) notEqualUsers);
    }

}
