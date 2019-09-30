package software.wings.search.entities.service;

import io.harness.beans.EmbeddedUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import software.wings.api.DeploymentType;
import software.wings.beans.EntityType;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.entities.related.deployment.RelatedDeploymentView;
import software.wings.search.framework.EntityBaseView;
import software.wings.search.framework.EntityInfo;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "ServiceViewKeys")
public class ServiceView extends EntityBaseView {
  private String appId;
  private String appName;
  private ArtifactType artifactType;
  private DeploymentType deploymentType;
  private Set<EntityInfo> workflows;
  private Set<EntityInfo> pipelines;
  private List<RelatedDeploymentView> deployments;
  private List<RelatedAuditView> audits;
  private List<Long> deploymentTimestamps;
  private List<Long> auditTimestamps;

  ServiceView(String uuid, String name, String description, String accountId, long createdAt, long lastUpdatedAt,
      EntityType entityType, EmbeddedUser createdBy, EmbeddedUser lastUpdatedBy, String appId,
      ArtifactType artifactType, DeploymentType deploymentType) {
    super(uuid, name, description, accountId, createdAt, lastUpdatedAt, entityType, createdBy, lastUpdatedBy);
    this.appId = appId;
    this.artifactType = artifactType;
    this.deploymentType = deploymentType;
  }
}
