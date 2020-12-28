package io.harness.beans.steps.stepinfo.publish.artifact.connectors;

import io.harness.pms.yaml.ParameterField;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("gcrConnector")
public class GcrConnector implements ArtifactConnector {
  @NotNull private ParameterField<String> connectorRef;
  @NotNull private ParameterField<String> location;
  @Builder.Default @NotNull Type type = Type.GCR;
}
