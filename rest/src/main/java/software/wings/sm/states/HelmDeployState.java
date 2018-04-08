package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import org.mongodb.morphia.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.HelmDeployStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.api.ServiceTemplateElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.DelegateTask;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;
import software.wings.helpers.ext.helm.response.ReleaseInfo;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.utils.ContainerDeploymentHelper;
import software.wings.utils.KubernetesConvention;
import software.wings.utils.Validator;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 3/25/18.
 */
public class HelmDeployState extends State {
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private DelegateService delegateService;
  @Inject private SecretManager secretManager;
  @Inject private SettingsService settingsService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ActivityService activityService;

  public static final String HELM_COMMAND_NAME = "Helm Deploy";
  private static final String DOCKER_IMAGE_TAG_PLACEHOLDER_REGEX = "\\$\\{DOCKER_IMAGE_TAG}";

  private static final Logger logger = LoggerFactory.getLogger(HelmDeployState.class);

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public HelmDeployState(String name) {
    super(name, StateType.HELM_DEPLOY.name());
  }

  public HelmDeployState(String name, String stateType) {
    super(name, stateType);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      return executeInternal(context);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, e).addParam("message", e.getMessage());
    }
  }

  protected ExecutionResponse executeInternal(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    Application app = appService.get(context.getAppId());
    Environment env = workflowStandardParams.getEnv();
    ServiceElement serviceElement = phaseElement.getServiceElement();
    Artifact artifact = ((DeploymentExecutionContext) context).getArtifactForService(serviceElement.getUuid());

    ContainerInfrastructureMapping containerInfraMapping =
        (ContainerInfrastructureMapping) infrastructureMappingService.get(
            app.getUuid(), phaseElement.getInfraMappingId());

    Activity activity = createActivity(context);

    String releaseName = KubernetesConvention.getHelmReleaseName(
        app.getName(), serviceElement.getName(), env.getName(), containerInfraMapping.getUuid());

    ContainerServiceParams containerServiceParams = getContainerServiceParams(containerInfraMapping, releaseName);

    HelmChartSpecification helmChartSpecification =
        serviceResourceService.getHelmChartSpecification(context.getAppId(), serviceElement.getUuid());

    validateChartSpecification(helmChartSpecification);

    HelmDeployStateExecutionData stateExecutionData = HelmDeployStateExecutionData.builder()
                                                          .activityId(activity.getUuid())
                                                          .chartName(helmChartSpecification.getChartName())
                                                          .chartRepositoryUrl(helmChartSpecification.getChartUrl())
                                                          .chartVersion(helmChartSpecification.getChartVersion())
                                                          .releaseName(releaseName)
                                                          .build();
    if (!isRollback()) {
      String prevVersion =
          getPreviousReleaseVersion(app.getUuid(), app.getAccountId(), releaseName, containerServiceParams);
      stateExecutionData.setReleaseOldVersion(prevVersion);
    }

    HelmCommandRequest commandRequest =
        getHelmCommandRequest(context, helmChartSpecification, containerServiceParams, releaseName, app.getAccountId(),
            app.getUuid(), activity.getUuid(), artifact.getBuildNo(), containerInfraMapping);

    DelegateTask delegateTask = aDelegateTask()
                                    .withAccountId(app.getAccountId())
                                    .withAppId(app.getUuid())
                                    .withTaskType(TaskType.HELM_COMMAND_TASK)
                                    .withWaitId(activity.getUuid())
                                    .withParameters(new Object[] {commandRequest})
                                    .withEnvId(env.getUuid())
                                    .withTimeout(TimeUnit.HOURS.toMillis(1))
                                    .withInfrastructureMappingId(containerInfraMapping.getUuid())
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);
    return ExecutionResponse.Builder.anExecutionResponse()
        .withCorrelationIds(Arrays.asList(activity.getUuid()))
        .withStateExecutionData(stateExecutionData)
        .withAsync(true)
        .build();
  }

  private void validateChartSpecification(HelmChartSpecification chartSpec) {
    if (chartSpec == null || (isEmpty(chartSpec.getChartName()) && isEmpty(chartSpec.getChartUrl()))) {
      throw new WingsException(ErrorCode.INVALID_REQUEST,
          "Invalid chart specification " + (chartSpec == null ? "NULL" : chartSpec.toString()));
    }
  }

  protected HelmCommandRequest getHelmCommandRequest(ExecutionContext context,
      HelmChartSpecification helmChartSpecification, ContainerServiceParams containerServiceParams, String releaseName,
      String accountId, String appId, String activityId, String imageTag,
      ContainerInfrastructureMapping infrastructureMapping) {
    List<String> helmValueOverridesYamlFiles =
        serviceTemplateService.helmValueOverridesYamlFiles(appId, infrastructureMapping.getServiceTemplateId());

    List<String> helmValueOverridesYamlFilesEvaluated = null;
    if (isNotEmpty(helmValueOverridesYamlFiles)) {
      helmValueOverridesYamlFilesEvaluated =
          helmValueOverridesYamlFiles.stream()
              .map(yamlFileContent -> yamlFileContent.replaceAll(DOCKER_IMAGE_TAG_PLACEHOLDER_REGEX, imageTag))
              .map(context::renderExpression)
              .collect(Collectors.toList());
    }

    return HelmInstallCommandRequest.builder()
        .appId(appId)
        .accountId(accountId)
        .activityId(activityId)
        .commandName(HELM_COMMAND_NAME)
        .chartSpecification(helmChartSpecification)
        .releaseName(releaseName)
        .namespace(infrastructureMapping.getNamespace())
        .containerServiceParams(containerServiceParams)
        .variableOverridesYamlFiles(helmValueOverridesYamlFilesEvaluated)
        .build();
  }

  protected String getPreviousReleaseVersion(
      String appId, String accountId, String releaseName, ContainerServiceParams containerServiceParams) {
    String prevVersion = null;
    try {
      HelmReleaseHistoryCommandRequest helmReleaseHistoryCommandRequest =
          HelmReleaseHistoryCommandRequest.builder()
              .releaseName(releaseName)
              .containerServiceParams(containerServiceParams)
              .build();
      HelmCommandExecutionResponse helmCommandExecutionResponse =
          delegateService.executeTask(aDelegateTask()
                                          .withTaskType(TaskType.HELM_COMMAND_TASK)
                                          .withParameters(new Object[] {helmReleaseHistoryCommandRequest})
                                          .withAccountId(accountId)
                                          .withAppId(appId)
                                          .withAsync(false)
                                          .build());
      if (helmCommandExecutionResponse != null
          && helmCommandExecutionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS)) {
        List<ReleaseInfo> releaseInfoList =
            ((HelmReleaseHistoryCommandResponse) helmCommandExecutionResponse.getHelmCommandResponse())
                .getReleaseInfoList();
        prevVersion = releaseInfoList.get(releaseInfoList.size() - 1).getRevision();
      }
    } catch (InterruptedException e) {
      logger.error("Helm Release history fetch failed", e);
    }
    return prevVersion;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    try {
      return handleAsyncInternal(context, response);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, e).addParam("message", e.getMessage());
    }
  }

  protected ExecutionResponse handleAsyncInternal(ExecutionContext context, Map<String, NotifyResponseData> response) {
    String activityId = response.keySet().iterator().next();
    HelmCommandExecutionResponse executionResponse = (HelmCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);
    HelmDeployStateExecutionData stateExecutionData = (HelmDeployStateExecutionData) context.getStateExecutionData();
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(executionResponse.getErrorMessage());

    List<InstanceStatusSummary> instanceStatusSummaries = getInstanceStatusSummaries(context, executionResponse);
    stateExecutionData.setNewInstanceStatusSummaries(instanceStatusSummaries);

    List<InstanceElement> instanceElements =
        instanceStatusSummaries.stream().map(InstanceStatusSummary::getInstanceElement).collect(toList());
    InstanceElementListParam instanceElementListParam =
        InstanceElementListParamBuilder.anInstanceElementListParam().withInstanceElements(instanceElements).build();

    return ExecutionResponse.Builder.anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withErrorMessage(executionResponse.getErrorMessage())
        .withStateExecutionData(stateExecutionData)
        .addContextElement(instanceElementListParam)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  protected List<InstanceStatusSummary> getInstanceStatusSummaries(
      ExecutionContext context, HelmCommandExecutionResponse executionResponse) {
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    ServiceElement serviceElement = phaseElement.getServiceElement();
    String serviceId = phaseElement.getServiceElement().getUuid();
    String appId = context.getAppId();
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams.getEnv().getUuid();

    Key<ServiceTemplate> serviceTemplateKey =
        serviceTemplateService.getTemplateRefKeysByService(appId, serviceId, envId).get(0);
    ServiceTemplateElement serviceTemplateElement = aServiceTemplateElement()
                                                        .withUuid(serviceTemplateKey.getId().toString())
                                                        .withServiceElement(serviceElement)
                                                        .build();

    HelmInstallCommandResponse helmCommandResponse =
        (HelmInstallCommandResponse) executionResponse.getHelmCommandResponse();
    instanceStatusSummaries.addAll(ContainerDeploymentHelper.getInstanceStatusSummaryFromContainerInfoList(
        helmCommandResponse.getContainerInfoList(), serviceTemplateElement));
    return instanceStatusSummaries;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected Activity createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .appId(app.getUuid())
                                          .commandName(HELM_COMMAND_NAME)
                                          .type(Type.Command)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(Collections.emptyList())
                                          .serviceVariables(Maps.newHashMap())
                                          .status(ExecutionStatus.RUNNING)
                                          .commandUnitType(CommandUnitType.HELM);

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType().equals(BUILD)) {
      activityBuilder.environmentId(GLOBAL_ENV_ID).environmentName(GLOBAL_ENV_ID).environmentType(ALL);
    } else {
      activityBuilder.environmentId(env.getUuid())
          .environmentName(env.getName())
          .environmentType(env.getEnvironmentType());
    }
    if (instanceElement != null) {
      activityBuilder.serviceTemplateId(instanceElement.getServiceTemplateElement().getUuid())
          .serviceTemplateName(instanceElement.getServiceTemplateElement().getName())
          .serviceId(instanceElement.getServiceTemplateElement().getServiceElement().getUuid())
          .serviceName(instanceElement.getServiceTemplateElement().getServiceElement().getName())
          .serviceInstanceId(instanceElement.getUuid())
          .hostName(instanceElement.getHost().getHostName());
    }

    Activity activity = activityBuilder.build();
    return activityService.save(activity);
  }

  public ContainerServiceParams getContainerServiceParams(
      ContainerInfrastructureMapping containerInfraMapping, String containerServiceName) {
    String clusterName = containerInfraMapping.getClusterName();
    SettingAttribute settingAttribute;
    String namespace = null;
    String region = null;
    String resourceGroup = null;
    String subscriptionId = null;
    if (containerInfraMapping instanceof DirectKubernetesInfrastructureMapping) {
      DirectKubernetesInfrastructureMapping directInfraMapping =
          (DirectKubernetesInfrastructureMapping) containerInfraMapping;
      settingAttribute = (directInfraMapping.getComputeProviderType().equals(SettingVariableTypes.DIRECT.name()))
          ? aSettingAttribute().withValue(directInfraMapping.createKubernetesConfig()).build()
          : settingsService.get(directInfraMapping.getComputeProviderSettingId());
      namespace = directInfraMapping.getNamespace();
    } else {
      settingAttribute = settingsService.get(containerInfraMapping.getComputeProviderSettingId());
      if (containerInfraMapping instanceof GcpKubernetesInfrastructureMapping) {
        namespace = ((GcpKubernetesInfrastructureMapping) containerInfraMapping).getNamespace();
      } else if (containerInfraMapping instanceof AzureKubernetesInfrastructureMapping) {
        subscriptionId = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getSubscriptionId();
        resourceGroup = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getResourceGroup();
        namespace = ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getNamespace();
      } else if (containerInfraMapping instanceof EcsInfrastructureMapping) {
        region = ((EcsInfrastructureMapping) containerInfraMapping).getRegion();
      }
    }
    Validator.notNullCheck("SettingAttribute", settingAttribute);

    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(
        (Encryptable) settingAttribute.getValue(), containerInfraMapping.getAppId(), null);
    return ContainerServiceParams.builder()
        .settingAttribute(settingAttribute)
        .containerServiceName(containerServiceName)
        .encryptionDetails(encryptionDetails)
        .clusterName(clusterName)
        .namespace(namespace)
        .region(region)
        .subscriptionId(subscriptionId)
        .resourceGroup(resourceGroup)
        .build();
  }
}
