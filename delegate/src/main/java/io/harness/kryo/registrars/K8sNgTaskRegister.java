/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.kryo.registrars;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.ProductModule;
import io.harness.azure.AzureEnvironmentType;
import io.harness.beans.KeyValuePair;
import io.harness.beans.NGInstanceUnitType;
import io.harness.delegate.beans.DelegateMetaInfo;
import io.harness.delegate.beans.DelegateStringResponseData;
import io.harness.delegate.beans.DelegateTaskNotifyResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.NotificationProcessingResponse;
import io.harness.delegate.beans.NotificationTaskResponse;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.SerializedResponseData;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsEqualJitterBackoffStrategySpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsFixedDelayBackoffStrategySpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsFullJitterBackoffStrategySpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsInheritFromDelegateSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsSdkClientBackoffStrategyDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsSdkClientBackoffStrategySpecDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsSdkClientBackoffStrategyType;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureAuthCredentialDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureAuthDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureClientKeyCertDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureClientSecretKeyDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialSpecDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialType;
import io.harness.delegate.beans.connector.azureconnector.AzureInheritFromDelegateDetailsDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureMSIAuthDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureMSIAuthSADTO;
import io.harness.delegate.beans.connector.azureconnector.AzureMSIAuthUADTO;
import io.harness.delegate.beans.connector.azureconnector.AzureManagedIdentityType;
import io.harness.delegate.beans.connector.azureconnector.AzureManualDetailsDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureSecretType;
import io.harness.delegate.beans.connector.azureconnector.AzureSystemAssignedMSIAuthDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureUserAssignedMSIAuthDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpDelegateDetailsDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthCredentialsDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthType;
import io.harness.delegate.beans.connector.helm.HttpHelmAuthenticationDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmUsernamePasswordDTO;
import io.harness.delegate.beans.connector.helm.OciHelmAuthCredentialsDTO;
import io.harness.delegate.beans.connector.helm.OciHelmAuthType;
import io.harness.delegate.beans.connector.helm.OciHelmAuthenticationDTO;
import io.harness.delegate.beans.connector.helm.OciHelmConnectorDTO;
import io.harness.delegate.beans.connector.helm.OciHelmUsernamePasswordDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClientKeyCertDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialSpecDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesOpenIdConnectDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesServiceAccountDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.delegate.beans.connector.pdcconnector.HostDTO;
import io.harness.delegate.beans.connector.pdcconnector.PhysicalDataCenterConnectorDTO;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitSSHAuthenticationDTO;
import io.harness.delegate.beans.executioncapability.AlwaysFalseValidationCapability;
import io.harness.delegate.beans.executioncapability.AwsCliInstallationCapability;
import io.harness.delegate.beans.executioncapability.AwsRegionCapability;
import io.harness.delegate.beans.executioncapability.AwsSamInstallationCapability;
import io.harness.delegate.beans.executioncapability.CIVmConnectionCapability;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ChartMuseumCapability;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.delegate.beans.executioncapability.GitInstallationCapability;
import io.harness.delegate.beans.executioncapability.HelmInstallationCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.KustomizeCapability;
import io.harness.delegate.beans.executioncapability.LiteEngineConnectionCapability;
import io.harness.delegate.beans.executioncapability.PcfAutoScalarCapability;
import io.harness.delegate.beans.executioncapability.PcfConnectivityCapability;
import io.harness.delegate.beans.executioncapability.PcfInstallationCapability;
import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.beans.executioncapability.ServerlessInstallationCapability;
import io.harness.delegate.beans.executioncapability.SftpCapability;
import io.harness.delegate.beans.executioncapability.SmbConnectionCapability;
import io.harness.delegate.beans.executioncapability.SmtpCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityBulkOrExecutionCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.delegate.beans.executioncapability.SshConnectivityExecutionCapability;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.beans.executioncapability.WinrmConnectivityExecutionCapability;
import io.harness.delegate.beans.helm.HelmDeployProgressData;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.storeconfig.ArtifactoryStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.CustomRemoteStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.beans.storeconfig.GcsHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.HarnessStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.InlineFileConfig;
import io.harness.delegate.beans.storeconfig.InlineStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.LocalFileStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.OciHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3StoreDelegateConfig;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.helm.HelmCommandFlag;
import io.harness.delegate.task.k8s.AzureK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.DeleteResourcesType;
import io.harness.delegate.task.k8s.DirectK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.EksK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.GcpK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.delegate.task.k8s.K8sApplyRequest;
import io.harness.delegate.task.k8s.K8sBGDeployRequest;
import io.harness.delegate.task.k8s.K8sBGDeployResponse;
import io.harness.delegate.task.k8s.K8sCanaryDeleteRequest;
import io.harness.delegate.task.k8s.K8sCanaryDeployRequest;
import io.harness.delegate.task.k8s.K8sCanaryDeployResponse;
import io.harness.delegate.task.k8s.K8sDeleteRequest;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sDryRunManifestRequest;
import io.harness.delegate.task.k8s.K8sDryRunManifestResponse;
import io.harness.delegate.task.k8s.K8sManifestDelegateConfig;
import io.harness.delegate.task.k8s.K8sRollingDeployRequest;
import io.harness.delegate.task.k8s.K8sRollingDeployResponse;
import io.harness.delegate.task.k8s.K8sRollingDeployRollbackResponse;
import io.harness.delegate.task.k8s.K8sRollingRollbackDeployRequest;
import io.harness.delegate.task.k8s.K8sScaleRequest;
import io.harness.delegate.task.k8s.K8sScaleResponse;
import io.harness.delegate.task.k8s.K8sSwapServiceSelectorsRequest;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.delegate.task.k8s.K8sTrafficRoutingRequest;
import io.harness.delegate.task.k8s.K8sTrafficRoutingResponse;
import io.harness.delegate.task.k8s.KustomizeManifestDelegateConfig;
import io.harness.delegate.task.k8s.OpenshiftManifestDelegateConfig;
import io.harness.delegate.task.k8s.RancherK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.ReleaseMetadata;
import io.harness.delegate.task.k8s.trafficrouting.HeaderConfig;
import io.harness.delegate.task.k8s.trafficrouting.IstioProviderConfig;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfig;
import io.harness.delegate.task.k8s.trafficrouting.K8sTrafficRoutingConfigType;
import io.harness.delegate.task.k8s.trafficrouting.MatchType;
import io.harness.delegate.task.k8s.trafficrouting.ProviderType;
import io.harness.delegate.task.k8s.trafficrouting.RouteType;
import io.harness.delegate.task.k8s.trafficrouting.RuleType;
import io.harness.delegate.task.k8s.trafficrouting.SMIProviderConfig;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoute;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRouteRule;
import io.harness.delegate.task.k8s.trafficrouting.TrafficRoutingDestination;
import io.harness.delegate.task.localstore.ManifestFiles;
import io.harness.delegate.task.ssh.AwsSshInfraDelegateConfig;
import io.harness.delegate.task.ssh.AwsWinrmInfraDelegateConfig;
import io.harness.delegate.task.ssh.AzureSshInfraDelegateConfig;
import io.harness.delegate.task.ssh.AzureWinrmInfraDelegateConfig;
import io.harness.delegate.task.ssh.EmptyHostDelegateConfig;
import io.harness.delegate.task.ssh.PdcSshInfraDelegateConfig;
import io.harness.delegate.task.ssh.PdcWinRmInfraDelegateConfig;
import io.harness.delegate.task.ssh.config.ConfigFileParameters;
import io.harness.delegate.task.ssh.config.SecretConfigFile;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.AdfsAuthException;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.ArtifactoryRegistryException;
import io.harness.exception.ArtifactoryServerException;
import io.harness.exception.AuthenticationException;
import io.harness.exception.AuthorizationException;
import io.harness.exception.AzureAKSException;
import io.harness.exception.AzureAuthenticationException;
import io.harness.exception.AzureConfigException;
import io.harness.exception.AzureContainerRegistryException;
import io.harness.exception.AzureServerException;
import io.harness.exception.ConnectException;
import io.harness.exception.ContextException;
import io.harness.exception.DataProcessingException;
import io.harness.exception.DelegateErrorHandlerException;
import io.harness.exception.DelegateNotAvailableException;
import io.harness.exception.EngineExpressionEvaluationException;
import io.harness.exception.EngineFunctorException;
import io.harness.exception.ExceptionHandlerNotFoundException;
import io.harness.exception.ExplanationException;
import io.harness.exception.FailureType;
import io.harness.exception.FunctorException;
import io.harness.exception.GcpServerException;
import io.harness.exception.GeneralException;
import io.harness.exception.GitOperationException;
import io.harness.exception.HintException;
import io.harness.exception.HttpResponseException;
import io.harness.exception.IllegalArgumentException;
import io.harness.exception.ImageNotFoundException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.InvalidCredentialsException;
import io.harness.exception.InvalidIdentifierRefException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidTagException;
import io.harness.exception.InvalidThirdPartyCredentialsException;
import io.harness.exception.InvalidYamlException;
import io.harness.exception.JiraClientException;
import io.harness.exception.KryoHandlerNotFoundException;
import io.harness.exception.KubernetesApiTaskException;
import io.harness.exception.KubernetesTaskException;
import io.harness.exception.KubernetesYamlException;
import io.harness.exception.NexusRegistryException;
import io.harness.exception.NexusServerException;
import io.harness.exception.SecretNotFoundException;
import io.harness.exception.ServiceNowException;
import io.harness.exception.ShellExecutionException;
import io.harness.exception.TerraformCloudException;
import io.harness.exception.TerraformCommandExecutionException;
import io.harness.exception.TerraformSecretCleanupFailureException;
import io.harness.exception.TerragruntCommandExecutionException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.UnresolvedExpressionsException;
import io.harness.exception.VerificationOperationException;
import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ReportTarget;
import io.harness.k8s.model.HelmVersion;
import io.harness.k8s.model.K8sContainer;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.CommandExecutionStatus;
import io.harness.manifest.CustomManifestSource;
import io.harness.ng.core.dto.secrets.KerberosConfigDTO;
import io.harness.ng.core.dto.secrets.KerberosWinRmConfigDTO;
import io.harness.ng.core.dto.secrets.NTLMConfigDTO;
import io.harness.ng.core.dto.secrets.SSHAuthDTO;
import io.harness.ng.core.dto.secrets.SSHConfigDTO;
import io.harness.ng.core.dto.secrets.SSHCredentialType;
import io.harness.ng.core.dto.secrets.SSHKeyPathCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeyReferenceCredentialDTO;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SSHPasswordCredentialDTO;
import io.harness.ng.core.dto.secrets.WinRmAuthDTO;
import io.harness.ng.core.dto.secrets.WinRmCommandParameter;
import io.harness.ng.core.dto.secrets.WinRmCredentialsSpecDTO;
import io.harness.pcf.model.CfCliVersion;
import io.harness.secretmanagerclient.SSHAuthScheme;
import io.harness.secretmanagerclient.ValueType;
import io.harness.secretmanagerclient.WinRmAuthScheme;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoRegistrar;

import software.wings.beans.TaskType;

import com.esotericsoftware.kryo.Kryo;
import java.util.LinkedHashSet;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_K8S})
public class K8sNgTaskRegister implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(FailureType.class, 1205);
    kryo.register(AzureEnvironmentType.class, 1436);
    kryo.register(NGInstanceUnitType.class, 1445);
    kryo.register(CustomManifestSource.class, 1449);
    kryo.register(VerificationOperationException.class, 3001);
    kryo.register(ServiceNowException.class, 3002);
    kryo.register(SecretRefData.class, 3003);
    kryo.register(Scope.class, 3004);
    kryo.register(GeneralException.class, 3005);
    kryo.register(KeyValuePair.class, 3008);
    kryo.register(TaskType.class, 5005);
    kryo.register(CommandExecutionStatus.class, 5037);
    kryo.register(RemoteMethodReturnValueData.class, 5122);
    kryo.register(EncryptedDataDetail.class, 5125);
    kryo.register(WingsException.class, 5174);
    kryo.register(ErrorNotifyResponseData.class, 5213);
    kryo.register(ErrorCode.class, 5233);
    kryo.register(WinRmCommandParameter.class, 5256);
    kryo.register(ExplanationException.class, 5324);
    kryo.register(HintException.class, 5325);
    kryo.register(InvalidArgumentsException.class, 5326);
    kryo.register(InvalidRequestException.class, 5327);
    kryo.register(UnauthorizedException.class, 5329);
    kryo.register(UnexpectedException.class, 5330);
    kryo.register(ReportTarget.class, 5348);
    kryo.register(DelegateMetaInfo.class, 5372);
    kryo.register(DelegateTaskNotifyResponseData.class, 5373);
    kryo.register(FunctorException.class, 5589);
    kryo.register(Level.class, 5590);
    kryo.register(K8sTaskType.class, 7125);
    kryo.register(K8sPod.class, 7145);
    kryo.register(K8sContainer.class, 7146);
    kryo.register(ArtifactoryServerException.class, 7214);
    kryo.register(ArtifactoryRegistryException.class, 7217);
    kryo.register(NexusServerException.class, 7218);
    kryo.register(ArtifactServerException.class, 7244);
    kryo.register(NexusRegistryException.class, 7246);
    kryo.register(InvalidArtifactServerException.class, 7250);
    kryo.register(HelmVersion.class, 7303);
    kryo.register(KustomizeCapability.class, 7437);
    kryo.register(ShellExecutionException.class, 7473);
    kryo.register(FetchType.class, 8030);
    kryo.register(HttpConnectionExecutionCapability.class, 19003);
    kryo.register(CapabilityType.class, 19004);
    kryo.register(ProcessExecutorCapability.class, 19007);
    kryo.register(AwsRegionCapability.class, 19008);
    kryo.register(SocketConnectivityExecutionCapability.class, 19009);
    kryo.register(SocketConnectivityBulkOrExecutionCapability.class, 19010);
    kryo.register(SystemEnvCheckerCapability.class, 19022);
    kryo.register(AlwaysFalseValidationCapability.class, 19036);
    kryo.register(ChartMuseumCapability.class, 19038);
    kryo.register(KubernetesClusterConfigDTO.class, 19045);
    kryo.register(KubernetesCredentialType.class, 19046);
    kryo.register(KubernetesCredentialSpecDTO.class, 19047);
    kryo.register(KubernetesDelegateDetailsDTO.class, 19048);
    kryo.register(KubernetesClusterDetailsDTO.class, 19049);
    kryo.register(KubernetesAuthDTO.class, 19050);
    kryo.register(KubernetesAuthType.class, 19051);
    kryo.register(KubernetesUserNamePasswordDTO.class, 19052);
    kryo.register(KubernetesClientKeyCertDTO.class, 19053);
    kryo.register(KubernetesServiceAccountDTO.class, 19054);
    kryo.register(KubernetesOpenIdConnectDTO.class, 19055);
    kryo.register(KubernetesAuthCredentialDTO.class, 19058);
    kryo.register(GitConfigDTO.class, 19060);
    kryo.register(GitAuthenticationDTO.class, 19063);
    kryo.register(GitHTTPAuthenticationDTO.class, 19064);
    kryo.register(GitSSHAuthenticationDTO.class, 19065);
    kryo.register(GitAuthType.class, 19066);
    kryo.register(GitConnectionType.class, 19068);
    kryo.register(SelectorCapability.class, 19098);
    kryo.register(K8sDeployResponse.class, 19099);
    kryo.register(K8sRollingDeployRequest.class, 19100);
    kryo.register(K8sDeployRequest.class, 19101);
    kryo.register(DirectK8sInfraDelegateConfig.class, 19102);
    kryo.register(K8sManifestDelegateConfig.class, 19103);
    kryo.register(GitStoreDelegateConfig.class, 19104);
    kryo.register(SmbConnectionCapability.class, 19119);
    kryo.register(HelmInstallationCapability.class, 19120);
    kryo.register(SmtpCapability.class, 19121);
    kryo.register(PcfAutoScalarCapability.class, 19122);
    kryo.register(PcfConnectivityCapability.class, 19123);
    kryo.register(SftpCapability.class, 19124);
    kryo.register(DelegateStringResponseData.class, 19309);
    kryo.register(K8sRollingDeployResponse.class, 19328);
    kryo.register(GitConnectionNGCapability.class, 19334);
    kryo.register(KubernetesCredentialDTO.class, 19342);
    kryo.register(GcpConnectorDTO.class, 19345);
    kryo.register(GcpConnectorCredentialDTO.class, 19346);
    kryo.register(GcpCredentialType.class, 19347);
    kryo.register(GcpDelegateDetailsDTO.class, 19349);
    kryo.register(GcpManualDetailsDTO.class, 19350);
    kryo.register(AwsConnectorDTO.class, 19351);
    kryo.register(AwsCredentialDTO.class, 19353);
    kryo.register(AwsCredentialSpecDTO.class, 19354);
    kryo.register(AwsCredentialType.class, 19355);
    kryo.register(AwsInheritFromDelegateSpecDTO.class, 19357);
    kryo.register(AwsManualConfigSpecDTO.class, 19358);
    kryo.register(CrossAccountAccessDTO.class, 19362);
    kryo.register(ConnectorType.class, 19372);
    kryo.register(K8sBGDeployRequest.class, 19435);
    kryo.register(K8sBGDeployResponse.class, 19436);
    kryo.register(K8sApplyRequest.class, 19437);
    kryo.register(HelmChartManifestDelegateConfig.class, 19548);
    kryo.register(GitInstallationCapability.class, 19550);
    kryo.register(HttpHelmStoreDelegateConfig.class, 19642);
    kryo.register(HttpHelmAuthCredentialsDTO.class, 19655);
    kryo.register(HttpHelmAuthenticationDTO.class, 19656);
    kryo.register(HttpHelmAuthType.class, 19657);
    kryo.register(HttpHelmConnectorDTO.class, 19658);
    kryo.register(HttpHelmUsernamePasswordDTO.class, 19659);
    kryo.register(AzureConnectorDTO.class, 19680);
    kryo.register(AzureCredentialSpecDTO.class, 19682);
    kryo.register(AzureManualDetailsDTO.class, 19683);
    kryo.register(AzureCredentialType.class, 19684);
    kryo.register(AzureCredentialDTO.class, 19685);
    kryo.register(AzureSecretType.class, 19686);
    kryo.register(AzureAuthCredentialDTO.class, 19687);
    kryo.register(AzureAuthDTO.class, 19688);
    kryo.register(AzureClientSecretKeyDTO.class, 19689);
    kryo.register(AzureClientKeyCertDTO.class, 19690);
    kryo.register(PhysicalDataCenterConnectorDTO.class, 19691);
    kryo.register(HostDTO.class, 19692);
    kryo.register(KustomizeManifestDelegateConfig.class, 19700);
    kryo.register(OpenshiftManifestDelegateConfig.class, 19701);
    kryo.register(S3HelmStoreDelegateConfig.class, 19702);
    kryo.register(GcsHelmStoreDelegateConfig.class, 19703);
    kryo.register(S3StoreDelegateConfig.class, 19706);
    kryo.register(AzureInheritFromDelegateDetailsDTO.class, 19805);
    kryo.register(AzureManagedIdentityType.class, 19806);
    kryo.register(AzureUserAssignedMSIAuthDTO.class, 19807);
    kryo.register(AzureMSIAuthDTO.class, 19808);
    kryo.register(AzureSystemAssignedMSIAuthDTO.class, 19809);
    kryo.register(AzureMSIAuthUADTO.class, 19810);
    kryo.register(AzureMSIAuthSADTO.class, 19811);
    kryo.register(OciHelmAuthCredentialsDTO.class, 29131);
    kryo.register(OciHelmAuthenticationDTO.class, 29132);
    kryo.register(OciHelmAuthType.class, 29133);
    kryo.register(OciHelmConnectorDTO.class, 29134);
    kryo.register(OciHelmUsernamePasswordDTO.class, 29135);
    kryo.register(OciHelmStoreDelegateConfig.class, 29308);
    kryo.register(DelegateErrorHandlerException.class, 31012);
    kryo.register(KryoHandlerNotFoundException.class, 31013);
    kryo.register(ExceptionHandlerNotFoundException.class, 31014);
    kryo.register(ImageNotFoundException.class, 31015);
    kryo.register(HttpResponseException.class, 31016);
    kryo.register(GcpServerException.class, 31017);
    kryo.register(InvalidCredentialsException.class, 31018);
    kryo.register(ContextException.class, 31019);
    kryo.register(InvalidTagException.class, 31020);
    kryo.register(SecretNotFoundException.class, 31021);
    kryo.register(DelegateNotAvailableException.class, 31022);
    kryo.register(IllegalArgumentException.class, 31023);
    kryo.register(InvalidThirdPartyCredentialsException.class, 31024);
    kryo.register(ConnectException.class, 31025);
    kryo.register(TerraformSecretCleanupFailureException.class, 40118);
    kryo.register(NotificationTaskResponse.class, 55216);
    kryo.register(NotificationProcessingResponse.class, 55217);
    kryo.register(PdcSshInfraDelegateConfig.class, 55308);
    kryo.register(HarnessStoreDelegateConfig.class, 55319);
    kryo.register(ConfigFileParameters.class, 55320);
    kryo.register(SecretConfigFile.class, 55334);
    kryo.register(PdcWinRmInfraDelegateConfig.class, 55335);
    kryo.register(SerializedResponseData.class, 55401);
    kryo.register(LocalFileStoreDelegateConfig.class, 55404);
    kryo.register(ManifestFiles.class, 55406);
    kryo.register(AzureSshInfraDelegateConfig.class, 55414);
    kryo.register(AzureWinrmInfraDelegateConfig.class, 55415);
    kryo.register(AwsSshInfraDelegateConfig.class, 55417);
    kryo.register(AwsWinrmInfraDelegateConfig.class, 55418);
    kryo.register(WinrmConnectivityExecutionCapability.class, 55425);
    kryo.register(SshConnectivityExecutionCapability.class, 55435);
    kryo.register(CustomRemoteStoreDelegateConfig.class, 56403);
    kryo.register(EmptyHostDelegateConfig.class, 60015);
    kryo.register(TaskGroup.class, 74001);
    kryo.register(UnitProgressData.class, 95001);
    kryo.register(HelmDeployProgressData.class, 95003);
    kryo.register(CfCliVersion.class, 97023);
    kryo.register(KubernetesResourceId.class, 97031);
    kryo.register(ValueType.class, 543215);
    kryo.register(SSHKeySpecDTO.class, 543222);
    kryo.register(SSHAuthScheme.class, 543223);
    kryo.register(SSHConfigDTO.class, 543224);
    kryo.register(SSHCredentialType.class, 543228);
    kryo.register(SSHKeyReferenceCredentialDTO.class, 543230);
    kryo.register(SSHPasswordCredentialDTO.class, 543231);
    kryo.register(SSHKeyPathCredentialDTO.class, 543232);
    kryo.register(KerberosConfigDTO.class, 543233);
    kryo.register(SSHAuthDTO.class, 543234);
    kryo.register(K8sRollingRollbackDeployRequest.class, 543239);
    kryo.register(K8sScaleRequest.class, 543240);
    kryo.register(K8sScaleResponse.class, 543241);
    kryo.register(K8sCanaryDeployRequest.class, 543249);
    kryo.register(K8sCanaryDeployResponse.class, 543250);
    kryo.register(K8sSwapServiceSelectorsRequest.class, 543254);
    kryo.register(K8sDeleteRequest.class, 543255);
    kryo.register(DeleteResourcesType.class, 543257);
    kryo.register(HelmCommandFlag.class, 543260);
    kryo.register(LiteEngineConnectionCapability.class, 543277);
    kryo.register(GcpK8sInfraDelegateConfig.class, 543278);
    kryo.register(CommandUnitsProgress.class, 543427);
    kryo.register(K8sCanaryDeleteRequest.class, 543429);
    kryo.register(K8sRollingDeployRollbackResponse.class, 543430);
    kryo.register(CIVmConnectionCapability.class, 543454);
    kryo.register(ArtifactoryStoreDelegateConfig.class, 543479);
    kryo.register(PcfInstallationCapability.class, 553289);
    kryo.register(ServerlessInstallationCapability.class, 563530);
    kryo.register(AzureK8sInfraDelegateConfig.class, 563532);
    kryo.register(AwsCliInstallationCapability.class, 563535);
    kryo.register(EksK8sInfraDelegateConfig.class, 563537);
    kryo.register(RancherK8sInfraDelegateConfig.class, 563538);
    kryo.register(InlineFileConfig.class, 573550);
    kryo.register(InlineStoreDelegateConfig.class, 573551);
    kryo.register(K8sDryRunManifestRequest.class, 573594);
    kryo.register(K8sDryRunManifestResponse.class, 573595);
    kryo.register(WinRmCredentialsSpecDTO.class, 600001);
    kryo.register(WinRmAuthScheme.class, 600002);
    kryo.register(NTLMConfigDTO.class, 600003);
    kryo.register(KerberosWinRmConfigDTO.class, 600004);
    kryo.register(WinRmAuthDTO.class, 600005);
    kryo.register(EngineExpressionEvaluationException.class, 980006);
    kryo.register(EngineFunctorException.class, 980007);
    kryo.register(UnresolvedExpressionsException.class, 980008);
    kryo.register(JiraClientException.class, 980009);
    kryo.register(InvalidYamlException.class, 980010);
    kryo.register(AuthenticationException.class, 980011);
    kryo.register(AuthorizationException.class, 980012);
    kryo.register(KubernetesApiTaskException.class, 980014);
    kryo.register(KubernetesTaskException.class, 980015);
    kryo.register(KubernetesYamlException.class, 980016);
    kryo.register(GitOperationException.class, 980017);
    kryo.register(TerraformCommandExecutionException.class, 980018);
    kryo.register(AzureServerException.class, 980021);
    kryo.register(AzureAuthenticationException.class, 980022);
    kryo.register(AzureConfigException.class, 980023);
    kryo.register(AzureContainerRegistryException.class, 980024);
    kryo.register(DataProcessingException.class, 980025);
    kryo.register(AzureAKSException.class, 980028);
    kryo.register(InvalidIdentifierRefException.class, 980031);
    kryo.register(AdfsAuthException.class, 10000120);
    kryo.register(TerragruntCommandExecutionException.class, 10000262);
    kryo.register(TerraformCloudException.class, 10000305);
    kryo.register(AwsSamInstallationCapability.class, 10000401);
    kryo.register(AwsFixedDelayBackoffStrategySpecDTO.class, 10000455);
    kryo.register(AwsEqualJitterBackoffStrategySpecDTO.class, 10000456);
    kryo.register(AwsFullJitterBackoffStrategySpecDTO.class, 10000457);
    kryo.register(AwsSdkClientBackoffStrategyDTO.class, 10000458);
    kryo.register(AwsSdkClientBackoffStrategySpecDTO.class, 10000459);
    kryo.register(AwsSdkClientBackoffStrategyType.class, 10000460);

    // k8s traffic routing
    kryo.register(HeaderConfig.class, 20002000);
    kryo.register(IstioProviderConfig.class, 20002001);
    kryo.register(K8sTrafficRoutingConfig.class, 20002002);
    kryo.register(SMIProviderConfig.class, 20002003);
    kryo.register(TrafficRoute.class, 20002004);
    kryo.register(TrafficRouteRule.class, 20002005);
    kryo.register(TrafficRoutingDestination.class, 20002006);
    kryo.register(MatchType.class, 20002007);
    kryo.register(ProviderType.class, 20002008);
    kryo.register(RuleType.class, 20002009);
    kryo.register(RouteType.class, 20002010);

    kryo.register(LinkedHashSet.class, 100030);
    kryo.register(TaskNGDataException.class, 543440);
    kryo.register(ReleaseMetadata.class, 20001002);
    kryo.register(K8sTrafficRoutingRequest.class, 20002012);
    kryo.register(K8sTrafficRoutingResponse.class, 20002013);
    kryo.register(K8sTrafficRoutingConfigType.class, 20002014);
  }
}
