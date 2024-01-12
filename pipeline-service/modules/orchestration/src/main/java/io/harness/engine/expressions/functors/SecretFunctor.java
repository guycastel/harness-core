/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.accesscontrol.NGAccessDeniedException;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.observers.SecretObserverInfo;
import io.harness.engine.observers.SecretResolutionObserver;
import io.harness.engine.utils.FunctorUtils;
import io.harness.eventsframework.protohelper.IdentifierRefProtoDTOHelper;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.EntityTypeProtoEnum;
import io.harness.expression.functors.ExpressionFunctor;
import io.harness.observer.Subject;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.security.PmsSecurityContextEventGuard;
import io.harness.utils.IdentifierRefHelper;

import java.util.Set;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Slf4j
@OwnedBy(CDC)
public class SecretFunctor implements ExpressionFunctor {
  Ambiance ambiance;
  Subject<SecretResolutionObserver> secretsRuntimeUsagesSubject;
  PipelineRbacHelper pipelineRbacHelper;

  public SecretFunctor(Ambiance ambiance, Subject<SecretResolutionObserver> secretsRuntimeUsagesSubject,
      PipelineRbacHelper pipelineRbacHelper) {
    this.ambiance = ambiance;
    this.pipelineRbacHelper = pipelineRbacHelper;
    this.secretsRuntimeUsagesSubject = secretsRuntimeUsagesSubject;
  }

  public Object getValue(String secretIdentifier) {
    if (EmptyPredicate.isNotEmpty(secretIdentifier) && ambiance != null
        && AmbianceUtils.shouldEnableSecretsObserver(ambiance)) {
      secretsRuntimeUsagesSubject.fireInform(SecretResolutionObserver::onSecretsRuntimeUsage,
          SecretObserverInfo.builder().secretIdentifier(secretIdentifier).ambiance(ambiance).build());
    }
    try (PmsSecurityContextEventGuard securityContextEventGuard = new PmsSecurityContextEventGuard(ambiance)) {
      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(secretIdentifier, AmbianceUtils.getAccountId(ambiance),
              AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));
      Set<EntityDetailProtoDTO> entityDetails =
          Set.of(EntityDetailProtoDTO.newBuilder()
                     .setType(EntityTypeProtoEnum.SECRETS)
                     .setIdentifierRef(IdentifierRefProtoDTOHelper.fromIdentifierRef(identifierRef))
                     .build());
      pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetails);

    } catch (NGAccessDeniedException exception) {
      log.error("Encountered NGAccessDenied error while resolving secret using SecretFunctor ", exception);
    } catch (Exception ex) {
      log.error("Encountered unknown error while resolving secret using SecretFunctor ", ex);
    }
    return FunctorUtils.getSecretExpression(ambiance.getExpressionFunctorToken(), secretIdentifier);
  }
}
