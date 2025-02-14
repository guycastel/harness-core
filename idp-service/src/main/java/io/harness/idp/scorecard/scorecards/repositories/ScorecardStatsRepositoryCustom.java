/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.scorecards.repositories;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.idp.backstage.entities.BackstageCatalogEntity;
import io.harness.idp.scorecard.scorecards.entity.ScorecardStatsEntity;
import io.harness.idp.scorecard.scores.entity.ScoreEntity;

import java.util.List;

@OwnedBy(HarnessTeam.IDP)
public interface ScorecardStatsRepositoryCustom {
  ScorecardStatsEntity findOneOrConstructStats(ScoreEntity scoreEntity, BackstageCatalogEntity backstageCatalog);

  List<ScorecardIdentifierAndScore> computeScoresPercentageByScorecard(
      String accountIdentifier, List<String> scorecardIdentifiers);
}
