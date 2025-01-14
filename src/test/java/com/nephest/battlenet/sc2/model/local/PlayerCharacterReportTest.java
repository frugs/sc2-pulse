// Copyright (C) 2020-2022 Oleksandr Masniuk
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.nephest.battlenet.sc2.model.local;

import com.nephest.battlenet.sc2.util.TestUtil;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

public class PlayerCharacterReportTest
{

    @Test
    public void testEquality()
    {
        OffsetDateTime equalDateTime = OffsetDateTime.now();
        PlayerCharacterReport report = new PlayerCharacterReport(0, 0L, 0L,
            PlayerCharacterReport.PlayerCharacterReportType.CHEATER, false, equalDateTime);
        PlayerCharacterReport equalReport = new PlayerCharacterReport(1, 0L, 0L,
            PlayerCharacterReport.PlayerCharacterReportType.CHEATER, true, equalDateTime.plusSeconds(1));
        PlayerCharacterReport[] notEqualReports = new PlayerCharacterReport[]
        {
            new PlayerCharacterReport(0, 1L, 0L,
                PlayerCharacterReport.PlayerCharacterReportType.CHEATER, false, equalDateTime),
            new PlayerCharacterReport(0, 0L, 1L,
                PlayerCharacterReport.PlayerCharacterReportType.CHEATER, false, equalDateTime),
            new PlayerCharacterReport(0, 0L, 0L,
                PlayerCharacterReport.PlayerCharacterReportType.LINK, false, equalDateTime)
        };

        TestUtil.testUniqueness(report, equalReport, (Object[]) notEqualReports);
    }

}
