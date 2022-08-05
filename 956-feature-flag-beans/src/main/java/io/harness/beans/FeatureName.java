/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag.Scope;

import lombok.Getter;

/**
 * Add your feature name here. When the feature is fully launched and no longer needs to be flagged,
 * delete the feature name.
 */
@OwnedBy(HarnessTeam.PL)
public enum FeatureName {
  DEPRECATE_K8S_STEADY_STATE_CHECK_STEP,
  NG_GITOPS,
  APPD_CV_TASK,
  ARGO_PHASE1,
  ARGO_PHASE2_MANAGED,
  ARTIFACT_PERPETUAL_TASK,
  ARTIFACT_PERPETUAL_TASK_MIGRATION,
  ARTIFACT_STREAM_REFACTOR,
  ARTIFACT_STREAM_DELEGATE_SCOPING,
  ARTIFACT_STREAM_DELEGATE_TIMEOUT,
  AUDIT_TRAIL_WEB_INTERFACE,
  AUTO_ACCEPT_SAML_ACCOUNT_INVITES,
  AZURE_US_GOV_CLOUD,
  AZURE_VMSS,
  AZURE_WEBAPP,
  AZURE_ARM,
  AUDIT_TRAIL_ENHANCEMENT,
  BIND_FETCH_FILES_TASK_TO_DELEGATE,
  BUSINESS_MAPPING("Cost Category Feature in CCM Module", HarnessTeam.CE),
  CCM_SUSTAINABILITY("Sustainability Feature in CCM Module", HarnessTeam.CE),
  CDNG_ENABLED,
  CENG_ENABLED("Enable the CCM module on NG", HarnessTeam.CE),
  CE_SAMPLE_DATA_GENERATION("Used to show sample data in CCM CG", HarnessTeam.CE),
  CE_HARNESS_ENTITY_MAPPING("Internal FF to decide if harness entities mapping is needed", HarnessTeam.CE),
  CE_HARNESS_INSTANCE_QUERY("Internal FF to decide which table to use for querying mapping data", HarnessTeam.CE),
  CFNG_ENABLED,
  CF_CUSTOM_EXTRACTION,
  CF_ROLLBACK_CONFIG_FILTER,
  CG_RBAC_EXCLUSION,
  CING_ENABLED,
  CI_INDIRECT_LOG_UPLOAD,
  CLOUD_FORMATION_CREATE_REFACTOR,
  CUSTOM_APM_24_X_7_CV_TASK,
  CUSTOM_APM_CV_TASK,
  CUSTOM_DASHBOARD,
  CUSTOM_DEPLOYMENT_ARTIFACT_FROM_INSTANCE_JSON,
  NG_DEPLOYMENT_TEMPLATE,
  CUSTOM_MAX_PAGE_SIZE,
  EXTRA_LARGE_PAGE_SIZE,
  CUSTOM_RESOURCEGROUP_SCOPE,
  CUSTOM_SECRETS_MANAGER,
  CVNG_ENABLED,
  CV_DEMO,
  CV_FEEDBACKS,
  CV_HOST_SAMPLING,
  CV_SUCCEED_FOR_ANOMALY,
  DEFAULT_ARTIFACT,
  DEPLOY_TO_SPECIFIC_HOSTS,
  ENABLE_ADDING_SERVICE_VARS_TO_ECS_SPEC,
  DISABLE_LOGML_NEURAL_NET,
  DISABLE_METRIC_NAME_CURLY_BRACE_CHECK,
  DISABLE_SERVICEGUARD_LOG_ALERTS,
  DISABLE_WINRM_COMMAND_ENCODING,
  ENABLE_WINRM_ENV_VARIABLES,
  FF_PIPELINE,
  FF_GITSYNC,
  FF_TEMPLATE_GITSYNC,
  FFM_1513,
  FFM_1512,
  FFM_1827,
  FFM_1859,
  FFM_2134_FF_PIPELINES_TRIGGER,
  FFM_3938_STALE_FLAGS_ACTIVE_CARD_HIDE_SHOW,
  FFM_4117_INTEGRATE_SRM("Enable Feature Flags to send events to the SRM module", HarnessTeam.CF),
  WINRM_COPY_CONFIG_OPTIMIZE,
  ECS_MULTI_LBS,
  ENTITY_AUDIT_RECORD,
  EXPORT_TF_PLAN,
  GCB_CI_SYSTEM,
  GCP_WORKLOAD_IDENTITY,
  GIT_ACCOUNT_SUPPORT,
  GIT_HTTPS_KERBEROS,
  GIT_HOST_CONNECTIVITY,
  GLOBAL_COMMAND_LIBRARY,
  GLOBAL_CV_DASH,
  GLOBAL_DISABLE_HEALTH_CHECK(Scope.GLOBAL),
  GRAPHQL_DEV,
  HARNESS_TAGS,
  HELM_CHART_AS_ARTIFACT,
  HELM_STEADY_STATE_CHECK_1_16,
  HELM_CHART_NAME_SPLIT,
  HELM_MERGE_CAPABILITIES,
  INLINE_SSH_COMMAND,
  IGNORE_PCF_CONNECTION_CONTEXT_CACHE,
  LIMIT_PCF_THREADS,
  OPA_FF_GOVERNANCE,
  OPA_GIT_GOVERNANCE,
  OPA_PIPELINE_GOVERNANCE,
  OPA_CONNECTOR_GOVERNANCE,
  OPA_SECRET_GOVERNANCE,
  PCF_OLD_APP_RESIZE,
  LOCAL_DELEGATE_CONFIG_OVERRIDE,
  LOGS_V2_247,
  MOVE_AWS_AMI_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_AMI_SPOT_INST_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_CODE_DEPLOY_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_LAMBDA_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_SSH_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_CONTAINER_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_PCF_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  PDC_PERPETUAL_TASK,
  NEW_RELIC_CV_TASK,
  NEWRELIC_24_7_CV_TASK,
  NG_DASHBOARDS("", HarnessTeam.CE),
  CI_TI_DASHBOARDS_ENABLED,
  NODE_RECOMMENDATION_AGGREGATE("K8S Node recommendation Feature in CCM", HarnessTeam.CE),
  ON_NEW_ARTIFACT_TRIGGER_WITH_LAST_COLLECTED_FILTER,
  OUTAGE_CV_DISABLE,
  OVERRIDE_VALUES_YAML_FROM_HELM_CHART,
  PIPELINE_GOVERNANCE,
  PRUNE_KUBERNETES_RESOURCES,
  REJECT_TRIGGER_IF_ARTIFACTS_NOT_MATCH,
  ROLLBACK_NONE_ARTIFACT,
  SCIM_INTEGRATION,
  SEARCH_REQUEST,
  SEND_LOG_ANALYSIS_COMPRESSED,
  SEND_SLACK_NOTIFICATION_FROM_DELEGATE,
  SIDE_NAVIGATION,
  SKIP_SWITCH_ACCOUNT_REAUTHENTICATION,
  SLACK_APPROVALS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_AMI_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_AMI_SPOT_INST_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_CODE_DEPLOY_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_LAMBDA_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_SSH_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_PDC_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AZURE_INFRA_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_CONTAINER_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_PCF_DEPLOYMENTS,
  SUPERVISED_TS_THRESHOLD,
  TEMPLATIZED_SECRET_MANAGER,
  THREE_PHASE_SECRET_DECRYPTION,
  TIME_RANGE_FREEZE_GOVERNANCE,
  TRIGGER_FOR_ALL_ARTIFACTS,
  TRIGGER_YAML,
  UI_ALLOW_K8S_V1,
  USE_NEXUS3_PRIVATE_APIS,
  WEEKLY_WINDOW,
  ENABLE_CVNG_INTEGRATION,
  DYNATRACE_MULTI_SERVICE,
  REFACTOR_STATEMACHINEXECUTOR,
  WORKFLOW_DATA_COLLECTION_ITERATOR,
  ENABLE_CERT_VALIDATION,
  RESOURCE_CONSTRAINT_MAX_QUEUE,
  RESOURCE_CONSTRAINT_SCOPE_PIPELINE_ENABLED,
  AWS_OVERRIDE_REGION,
  CLEAN_UP_OLD_MANAGER_VERSIONS(Scope.PER_ACCOUNT),
  ECS_AUTOSCALAR_REDESIGN,
  SAVE_SHELL_SCRIPT_PROVISION_OUTPUTS_TO_SWEEPING_OUTPUT,
  SAVE_TERRAFORM_OUTPUTS_TO_SWEEPING_OUTPUT,
  SAVE_TERRAFORM_APPLY_SWEEPING_OUTPUT_TO_WORKFLOW,
  TRIGGER_PROFILE_SCRIPT_EXECUTION_WF,
  NEW_DEPLOYMENT_FREEZE,
  ECS_REGISTER_TASK_DEFINITION_TAGS,
  CUSTOM_DASHBOARD_INSTANCE_FETCH_LONGER_RETENTION_DATA,
  CUSTOM_DASHBOARD_DEPLOYMENT_FETCH_LONGER_RETENTION_DATA,
  CUSTOM_DASHBOARD_ENABLE_REALTIME_INSTANCE_AGGREGATION,
  CUSTOM_DASHBOARD_ENABLE_REALTIME_DEPLOYMENT_MIGRATION,
  CUSTOM_DASHBOARD_ENABLE_CRON_INSTANCE_DATA_MIGRATION,
  CUSTOM_DASHBOARD_ENABLE_CRON_DEPLOYMENT_DATA_MIGRATION,
  SSH_SECRET_ENGINE,
  WHITELIST_PUBLIC_API,
  WHITELIST_GRAPHQL,
  TIMEOUT_FAILURE_SUPPORT,
  LOG_APP_DEFAULTS,
  ENABLE_LOGIN_AUDITS,
  CUSTOM_MANIFEST,
  WEBHOOK_TRIGGER_AUTHORIZATION,
  ENHANCED_GCR_CONNECTIVITY_CHECK,
  USE_TF_CLIENT,
  SERVICE_DASHBOARD_NG,
  GITHUB_WEBHOOK_AUTHENTICATION,
  NG_SIGNUP(Scope.GLOBAL),
  NG_LICENSES_ENABLED,
  ECS_BG_DOWNSIZE,
  LIMITED_ACCESS_FOR_HARNESS_USER_GROUP,
  REMOVE_STENCIL_MANUAL_INTERVENTION,
  CI_OVERVIEW_PAGE("UI flag to show CI overview page", HarnessTeam.CI),
  SKIP_BASED_ON_STACK_STATUSES,
  WF_VAR_MULTI_SELECT_ALLOWED_VALUES,
  LDAP_GROUP_SYNC_JOB_ITERATOR,
  PIPELINE_MONITORING,
  CF_CLI7,
  CF_APP_NON_VERSIONING_INACTIVE_ROLLBACK,
  CF_ALLOW_SPECIAL_CHARACTERS,
  HTTP_HEADERS_CAPABILITY_CHECK,
  AMI_IN_SERVICE_HEALTHY_WAIT,
  SETTINGS_OPTIMIZATION,
  CG_SECRET_MANAGER_DELEGATE_SELECTORS,
  ARTIFACT_COLLECTION_CONFIGURABLE,
  ROLLBACK_PROVISIONER_AFTER_PHASES,
  PLANS_ENABLED,
  FEATURE_ENFORCEMENT_ENABLED,
  FREE_PLAN_ENFORCEMENT_ENABLED,
  FREE_PLAN_ENABLED,
  VIEW_USAGE_ENABLED,
  SOCKET_HTTP_STATE_TIMEOUT,
  TERRAFORM_CONFIG_INSPECT_VERSION_SELECTOR,
  VALIDATE_PROVISIONER_EXPRESSION,
  WORKFLOW_PIPELINE_PERMISSION_BY_ENTITY,
  AMAZON_ECR_AUTH_REFACTOR,
  AMI_ASG_CONFIG_COPY,
  OPTIMIZED_GIT_FETCH_FILES,
  CVNG_VERIFY_STEP_DEMO,
  CVNG_MONITORED_SERVICE_DEMO,
  CVNG_VERIFY_STEP_LOGS_UI_V2,
  MANIFEST_INHERIT_FROM_CANARY_TO_PRIMARY_PHASE,
  USE_LATEST_CHARTMUSEUM_VERSION,
  KUBERNETES_EXPORT_MANIFESTS,
  NG_TEMPLATES,
  NEW_KUSTOMIZE_BINARY,
  KUSTOMIZE_PATCHES_CG,
  SSH_JSCH_LOGS,
  RESOLVE_DEPLOYMENT_TAGS_BEFORE_EXECUTION,
  LDAP_USER_ID_SYNC,
  NEW_KUBECTL_VERSION,
  CUSTOM_DASHBOARD_V2, // To be used only by ui to control flow from cg dashbaords to ng
  TIME_SCALE_CG_SYNC,
  CI_INCREASE_DEFAULT_RESOURCES,
  DISABLE_DEPLOYMENTS_SEARCH_AND_LIMIT_DEPLOYMENT_STATS,
  RATE_LIMITED_TOTP,
  USE_HELM_REPO_FLAGS,
  CLOSE_TIME_SCALE_SYNC_PROCESSING_ON_FAILURE(Scope.GLOBAL),
  RESOURCE_CENTER_ENABLED,
  USE_IMMUTABLE_DELEGATE("Use immutable delegate on download delegate from UI", HarnessTeam.DEL),
  ACTIVE_MIGRATION_FROM_LOCAL_TO_GCP_KMS,
  TERRAFORM_AWS_CP_AUTHENTICATION,
  CI_VM_INFRASTRUCTURE,
  SERVICENOW_NG_INTEGRATION,
  OPTIMIZED_TF_PLAN,
  SELF_SERVICE_ENABLED,
  CHI_CUSTOM_HEALTH,
  CHI_CUSTOM_HEALTH_LOGS,
  AZURE_SAML_150_GROUPS_SUPPORT,
  CLOUDFORMATION_SKIP_WAIT_FOR_RESOURCES,
  CLOUDFORMATION_CHANGE_SET,
  FAIL_WORKFLOW_IF_SECRET_DECRYPTION_FAILS,
  ERROR_TRACKING_ENABLED,
  DEPLOY_TO_INLINE_HOSTS,
  HONOR_DELEGATE_SCOPING,
  CG_LICENSE_USAGE,
  RANCHER_SUPPORT,
  BYPASS_HELM_FETCH,
  FREEZE_DURING_MIGRATION,
  USE_ANALYTIC_MONGO_FOR_GRAPHQL_QUERY,
  DYNATRACE_APM_ENABLED,
  CUSTOM_POLICY_STEP,
  KEEP_PT_AFTER_K8S_DOWNSCALE,
  CCM_AS_DRY_RUN("Dry Run functionality of the AutoStopping Rules", HarnessTeam.CE),
  DONT_RESTRICT_PARALLEL_STAGE_COUNT,
  NG_EXECUTION_INPUT,
  HELM_CHART_VERSION_STRICT_MATCH,
  SKIP_ADDING_TRACK_LABEL_SELECTOR_IN_ROLLING,
  EXTERNAL_USERID_BASED_LOGIN,
  LDAP_SYNC_WITH_USERID,
  DISABLE_HARNESS_SM,
  SECURITY("Enable the STO module on NG", HarnessTeam.STO),
  SECURITY_STAGE("Enable the Security Tests stage on NG", HarnessTeam.STO),
  STO_CI_PIPELINE_SECURITY("Enable the Security Tests execution results tab for CI on NG", HarnessTeam.STO),
  STO_CD_PIPELINE_SECURITY("Enable the Security Tests execution results tab for CD on NG", HarnessTeam.STO),
  STO_API_V2("Enable the new STO API version on NG", HarnessTeam.STO),
  GIT_SYNC_WITH_BITBUCKET,
  REFACTOR_ARTIFACT_SELECTION,
  CCM_DEV_TEST("", HarnessTeam.CE),
  CV_FAIL_ON_EMPTY_NODES,
  SHOW_REFINER_FEEDBACK,
  SHOW_NG_REFINER_FEEDBACK,
  NG_NEXUS_ARTIFACTORY,
  HELM_VERSION_3_8_0,
  DELETE_HELM_REPO_CACHE_DIR,
  DELEGATE_ENABLE_DYNAMIC_HANDLING_OF_REQUEST("Enable dynamic handling of task request", HarnessTeam.DEL),
  YAML_GIT_CONNECTOR_NAME,
  STOP_SHOWING_RUNNING_EXECUTIONS,
  SSH_NG,
  ARTIFACT_STREAM_METADATA_ONLY,
  SERVICENOW_CREATE_UPDATE_NG,
  OUTCOME_GRAPHQL_WITH_INFRA_DEF,
  AUTO_REJECT_PREVIOUS_APPROVALS,
  ENABLE_K8S_AUTH_IN_VAULT,
  BIND_CUSTOM_VALUE_AND_MANIFEST_FETCH_TASK,
  AZURE_BLOB_SM,
  CONSIDER_ORIGINAL_STATE_VERSION,
  SINGLE_MANIFEST_SUPPORT,
  GIT_SYNC_PROJECT_CLEANUP,
  ENV_GROUP,
  REDUCE_DELEGATE_MEMORY_SIZE("Reduce CG delegate memory to 4GB", HarnessTeam.DEL),
  NG_VARIABLES,
  PIPELINE_PER_ENV_DEPLOYMENT_PERMISSION,
  DISABLE_LOCAL_LOGIN,
  WINRM_KERBEROS_CACHE_UNIQUE_FILE,
  HIDE_ABORT,
  CUSTOM_ARTIFACT_NG,
  NG_TEMPLATE_REFERENCES_SUPPORT,
  APPLICATION_DROPDOWN_MULTISELECT,
  NG_AZURE,
  NG_GIT_EXPERIENCE,
  CIE_HOSTED_BUILDS,
  LDAP_SECRET_AUTH,
  WORKFLOW_EXECUTION_REFRESH_STATUS,
  SERVERLESS_SUPPORT,
  TRIGGERS_PAGE_PAGINATION,
  CVNG_NOTIFICATION_UI,
  STALE_FLAGS_FFM_1510,
  NG_SVC_ENV_REDESIGN,
  NEW_PIPELINE_STUDIO,
  EARLY_ACCESS_ENABLED,
  AZURE_REPO_CONNECTOR,
  HELM_OCI_SUPPORT,
  HELP_PANEL,
  CHAOS_ENABLED,
  DEPLOYMENT_SUBFORMIK_APPLICATION_DROPDOWN,
  USAGE_SCOPE_RBAC,
  ALLOW_USER_TYPE_FIELDS_JIRA,
  HARD_DELETE_ENTITIES,
  PIPELINE_MATRIX,
  ACTIVITY_ID_BASED_TF_BASE_DIR,
  INHERITED_USER_GROUP,
  JDK11_UPGRADE_BANNER,
  DISABLE_CI_STAGE_DEL_SELECTOR,
  CLEANUP_INCOMPLETE_CANARY_DEPLOY_RELEASE,
  JENKINS_ARTIFACT,
  ENABLE_DEFAULT_TIMEFRAME_IN_DEPLOYMENTS,
  EXPORT_TF_PLAN_JSON_NG,
  ADD_MANIFEST_COLLECTION_STEP,
  NG_CUSTOM_APPROVAL,
  NG_FILE_STORE,
  ACCOUNT_BASIC_ROLE,
  CVNG_TEMPLATE_MONITORED_SERVICE,
  CVNG_TEMPLATE_VERIFY_STEP,
  CVNG_METRIC_THRESHOLD,
  WORKFLOW_EXECUTION_ZOMBIE_MONITOR,
  PIPELINE_QUEUE_STEP,
  USE_PAGINATED_ENCRYPT_SERVICE, // To be only used by UI for safeguarding encrypt component changes in CG
  INFRA_MAPPING_BASED_ROLLBACK_ARTIFACT,
  DEPLOYMENT_SUBFORMIK_PIPELINE_DROPDOWN,
  DEPLOYMENT_SUBFORMIK_WORKFLOW_DROPDOWN,
  TI_DOTNET,
  TG_USE_AUTO_APPROVE_FLAG,
  CVNG_SPLUNK_METRICS,
  AUTO_FREE_MODULE_LICENSE,
  SRM_LICENSE_ENABLED,
  AZURE_WEBAPP_NG,
  ACCOUNT_BASIC_ROLE_ONLY,
  SEARCH_USERGROUP_BY_APPLICATION("Search in usergroup by application in CG", HarnessTeam.SPG),
  GITOPS_BYO_ARGO,
  CCM_MICRO_FRONTEND("Micro front for CCM", HarnessTeam.CE),
  NG_GIT_EXPERIENCE_IMPORT_FLOW,
  CVNG_LICENSE_ENFORCEMENT,
  SERVICE_DASHBOARD_V2,
  DEBEZIUM_ENABLED,
  YAML_APIS_GRANULAR_PERMISSION,
  JENKINS_BUILD,
  ENABLE_DEFAULT_NG_EXPERIENCE_FOR_ONPREM,
  NG_SETTINGS("Enable Settings at various scopes in NG", HarnessTeam.PL),
  QUEUED_COUNT_FOR_QUEUEKEY("Used to display the count of the queue in CG git sync", HarnessTeam.SPG),
  NG_EMAIL_STEP,
  PRUNE_KUBERNETES_RESOURCES_NG,
  DISABLE_PIPELINE_SCHEMA_VALIDATION(
      "Used to disable pipeline yaml schema as We saw some intermittent issue in Schema Validation due to invalid schema generation. Will keep this FF until root cause is found and fixed.",
      HarnessTeam.PIPELINE),
  CI_STEP_GROUP_ENABLED,
  GIT_SIMPLIFICATION_DISABLED,
  USE_K8S_API_FOR_STEADY_STATE_CHECK,
  WINRM_ASG_ROLLBACK("Used for Collect remaining instances rollback step", HarnessTeam.CDP),

  SAVE_ARTIFACT_TO_DB("Saves artifact to db and proceed in artifact collection step if not found", HarnessTeam.CDC),
  NG_INLINE_MANIFEST,
  NG_CUSTOM_REMOTE_MANIFEST,
  CI_DISABLE_RESOURCE_OPTIMIZATION(
      "Used for disabling the resource optimization, AXA had asked this flag", HarnessTeam.CI),
  ENABLE_EXPERIMENTAL_STEP_FAILURE_STRATEGIES(
      "Used to enable rollback workflow strategy on step failure", HarnessTeam.SPG),
  COMPARE_YAML_IN_GIT_SYNC(
      "Compare Yaml of two entities while git-sync (as of now only for application access)", HarnessTeam.SPG),
  REMOVE_USERGROUP_CHECK(
      "Customers started facing NPE due to migration of usergroup reference, removed null check behind FF - ticket ID - CDS-39770, CG",
      HarnessTeam.SPG),
  HOSTED_BUILDS("Used to enabled Hosted builds in paid accounts", HarnessTeam.CI),
  CD_ONBOARDING_ENABLED,
  ATTRIBUTE_TYPE_ACL_ENABLED("Enable attribute filter on NG UI for ACL", HarnessTeam.PL),
  CREATE_DEFAULT_PROJECT("Enables auto create default project after user signup", HarnessTeam.GTM),
  ANALYSE_TF_PLAN_SUMMARY(
      "Enables parsing of the Terraform plan/apply/destroy summary [add/change/destroy] and exposing them as expressions",
      HarnessTeam.CDP),
  TERRAFORM_REMOTE_BACKEND_CONFIG("Enables storing Terraform backend configuration in a remote repo", HarnessTeam.CDP),
  NG_OPTIMIZE_FETCH_FILES_KUSTOMIZE("Used to Optimize kustomize Manifest files fetch in NG", HarnessTeam.CDP),
  REMOVE_HINT_YAML_GIT_COMMITS("Removes the hint usage in GitCommits collection", HarnessTeam.SPG),
  FIXED_INSTANCE_ZERO_ALLOW("To allow user to set the fixed instance count to 0 for ECS Deployments", HarnessTeam.CDP),
  USE_PAGINATED_ENCRYPT_FOR_VARIABLE_OVERRIDES(
      "Enables PaginatedComponent & Formik for VariableOverrides in CG-UI", HarnessTeam.PL),
  ON_DEMAND_ROLLBACK_WITH_DIFFERENT_ARTIFACT(
      "Used to do on demand rollback to previously deployed different artifact on same inframapping", HarnessTeam.CDC);
  @Deprecated
  FeatureName() {
    scope = Scope.PER_ACCOUNT;
  }

  @Deprecated
  FeatureName(Scope scope) {
    this.scope = scope;
  }

  @Getter private FeatureFlag.Scope scope;

  FeatureName(String description, HarnessTeam owner) {
    this.description = description;
    this.owner = owner;
    this.scope = Scope.PER_ACCOUNT;
  }

  @Getter private String description;
  private HarnessTeam owner;

  public String getOwner() {
    return owner.name();
  }
}
