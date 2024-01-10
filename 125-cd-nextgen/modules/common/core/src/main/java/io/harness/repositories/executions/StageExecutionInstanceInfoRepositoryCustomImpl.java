/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.executions;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.execution.StageExecutionInstanceInfo;
import io.harness.cdng.execution.StageExecutionInstanceInfo.StageExecutionInstanceInfoKeys;
import io.harness.delegate.cdng.execution.StepExecutionInstanceInfo;

import com.google.inject.Inject;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.CDP)
public class StageExecutionInstanceInfoRepositoryCustomImpl implements StageExecutionInstanceInfoRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public StageExecutionInstanceInfo append(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String pipelineExecutionId, String stageExecutionId, StepExecutionInstanceInfo stepExecutionInstanceInfo,
      String stepPath) {
    Query query = new Query();
    query.addCriteria(
        createCriteria(accountIdentifier, orgIdentifier, projectIdentifier, pipelineExecutionId, stageExecutionId));
    StageExecutionInstanceInfo instanceInfo = mongoTemplate.findOne(query, StageExecutionInstanceInfo.class);

    if (null != instanceInfo) {
      Update update =
          new Update().set(StageExecutionInstanceInfoKeys.instanceInfoMap + "." + stepPath, stepExecutionInstanceInfo);
      return mongoTemplate.findAndModify(
          query, update, FindAndModifyOptions.options().upsert(true), StageExecutionInstanceInfo.class);
    } else {
      StageExecutionInstanceInfo newInstanceInfo = StageExecutionInstanceInfo.builder()
                                                       .accountIdentifier(accountIdentifier)
                                                       .orgIdentifier(orgIdentifier)
                                                       .projectIdentifier(projectIdentifier)
                                                       .pipelineExecutionId(pipelineExecutionId)
                                                       .stageExecutionId(stageExecutionId)
                                                       .instanceInfoMap(Map.of(stepPath, stepExecutionInstanceInfo))
                                                       .build();

      return mongoTemplate.save(newInstanceInfo);
    }
  }

  private Criteria createCriteria(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String pipelineExecutionId, String stageExecutionId) {
    Criteria criteria = new Criteria();
    criteria.and(StageExecutionInstanceInfoKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(StageExecutionInstanceInfoKeys.orgIdentifier).is(orgIdentifier);
    criteria.and(StageExecutionInstanceInfoKeys.projectIdentifier).is(projectIdentifier);
    criteria.and(StageExecutionInstanceInfoKeys.pipelineExecutionId).is(pipelineExecutionId);
    criteria.and(StageExecutionInstanceInfoKeys.stageExecutionId).is(stageExecutionId);
    return criteria;
  }
}
