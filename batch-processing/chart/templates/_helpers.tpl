{{/*
Expand the name of the chart.
*/}}
{{- define "batch-processing.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "batch-processing.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "batch-processing.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "batch-processing.labels" -}}
helm.sh/chart: {{ include "batch-processing.chart" . }}
{{ include "batch-processing.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "batch-processing.selectorLabels" -}}
app.kubernetes.io/name: {{ include "batch-processing.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Common labels for CCM Azure SMP K8s Cron Job
*/}}
{{- define "ccm-azure-smp.labels" -}}
helm.sh/chart: {{ include "batch-processing.chart" . }}
{{ include "ccm-azure-smp.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels for CCM Azure SMP K8s Cron Job
*/}}
{{- define "ccm-azure-smp.selectorLabels" -}}
app.kubernetes.io/name: ccm-smp-azure
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "batch-processing.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "batch-processing.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{- define "batch-processing.deploymentEnv" -}}
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
        name: postgres
        key: postgres-password
- { name: APP_DATABASE_DATASOURCE, value: "{{ printf "postgres://postgres:$(DB_PASSWORD)@postgres:5432" }}" }
- { name: APP_DB_MIGRATION_DATASOURCE, value: "{{ printf "postgres://postgres:$(DB_PASSWORD)@postgres:5432" }}" }
{{- end }}

{{- define "batch-processing.generateSecrets" }}
    {{- $ := .ctx }}
    {{- $hasAtleastOneSecret := false }}
    {{- $localESOSecretCtxIdentifier := (include "harnesscommon.secrets.localESOSecretCtxIdentifier" (dict "ctx" $ )) }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "S3_SYNC_CONFIG_ACCESSKEY")) "true" }}
    {{- $hasAtleastOneSecret = true }}
S3_SYNC_CONFIG_ACCESSKEY: {{ include "harnesscommon.secrets.passwords.manage" (dict "secret" "batch-processing" "key" "S3_SYNC_CONFIG_ACCESSKEY" "providedValues" (list "awsSecret.S3_SYNC_CONFIG_ACCESSKEY" "secrets.default.S3_SYNC_CONFIG_ACCESSKEY") "length" 10 "context" $) }}
    {{- end }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "S3_SYNC_CONFIG_SECRETKEY")) "true" }}
    {{- $hasAtleastOneSecret = true }}
S3_SYNC_CONFIG_SECRETKEY: {{ include "harnesscommon.secrets.passwords.manage" (dict "secret" "batch-processing" "key" "S3_SYNC_CONFIG_SECRETKEY" "providedValues" (list "awsSecret.S3_SYNC_CONFIG_SECRETKEY" "secrets.default.S3_SYNC_CONFIG_SECRETKEY") "length" 10 "context" $) }}
    {{- end }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "NEXT_GEN_MANAGER_SECRET")) "true" }}
    {{- $hasAtleastOneSecret = true }}
NEXT_GEN_MANAGER_SECRET: {{ include "harnesscommon.secrets.passwords.manage" (dict "secret" "batch-processing" "key" "NEXT_GEN_MANAGER_SECRET" "providedValues" (list "secrets.default.NEXT_GEN_MANAGER_SECRET") "length" 10 "context" $) }}
    {{- end }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "CE_NG_SERVICE_SECRET")) "true" }}
    {{- $hasAtleastOneSecret = true }}
CE_NG_SERVICE_SECRET: {{ include "harnesscommon.secrets.passwords.manage" (dict "secret" "batch-processing" "key" "CE_NG_SERVICE_SECRET" "providedValues" (list "secrets.default.CE_NG_SERVICE_SECRET") "length" 10 "context" $) }}
    {{- end }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "HARNESS_CE_AZURE_CLIENTSECRET")) "true" }}
    {{- $hasAtleastOneSecret = true }}
HARNESS_CE_AZURE_CLIENTSECRET: {{ include "harnesscommon.secrets.passwords.manage" (dict "secret" "batch-processing" "key" "HARNESS_CE_AZURE_CLIENTSECRET" "providedValues" (list "secrets.default.HARNESS_CE_AZURE_CLIENTSECRET") "length" 10 "context" $) }}
    {{- end }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "HARNESS_CE_AZURE_SAS")) "true" }}
    {{- $hasAtleastOneSecret = true }}
HARNESS_CE_AZURE_SAS: {{ include "harnesscommon.secrets.passwords.manage" (dict "secret" "batch-processing" "key" "HARNESS_CE_AZURE_SAS" "providedValues" (list "secrets.default.HARNESS_CE_AZURE_SAS") "length" 10 "context" $) }}
    {{- end }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "HARNESS_CE_AZURE_CLIENTID")) "true" }}
    {{- $hasAtleastOneSecret = true }}
HARNESS_CE_AZURE_CLIENTID: {{ include "harnesscommon.secrets.passwords.manage" (dict "secret" "batch-processing" "key" "HARNESS_CE_AZURE_CLIENTID" "providedValues" (list "secrets.default.HARNESS_CE_AZURE_CLIENTID") "length" 10 "context" $) }}
    {{- end }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "HARNESS_CE_AZURE_TENANTID")) "true" }}
    {{- $hasAtleastOneSecret = true }}
HARNESS_CE_AZURE_TENANTID: {{ include "harnesscommon.secrets.passwords.manage" (dict "secret" "batch-processing" "key" "HARNESS_CE_AZURE_TENANTID" "providedValues" (list "secrets.default.HARNESS_CE_AZURE_TENANTID") "length" 10 "context" $) }}
    {{- end }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "HMAC_ACCESS_KEY")) "true" }}
    {{- $hasAtleastOneSecret = true }}
HMAC_ACCESS_KEY: {{ include "harnesscommon.secrets.passwords.manage" (dict "secret" "batch-processing" "key" "HMAC_ACCESS_KEY" "providedValues" (list "secrets.default.HMAC_ACCESS_KEY") "length" 10 "context" $) }}
    {{- end }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "HMAC_SECRET_KEY")) "true" }}
    {{- $hasAtleastOneSecret = true }}
HMAC_SECRET_KEY: {{ include "harnesscommon.secrets.passwords.manage" (dict "secret" "batch-processing" "key" "HMAC_SECRET_KEY" "providedValues" (list "secrets.default.HMAC_SECRET_KEY") "length" 10 "context" $) }}
    {{- end }}
    {{- if not $hasAtleastOneSecret }}
{}
    {{- end }}
{{- end }}

{{- define "batch-processing.generateMountSecrets" }}
    {{- if not .Values.workloadIdentity.enabled }}
    ce-batch-gcp-credentials: {{ include "harnesscommon.secrets.passwords.manage" (dict "secret" "batch-processing-secret-mount" "key" "ce-batch-gcp-credentials" "providedValues" (list "ceBatchGCPCredentials") "length" 10 "context" $) }}
    ce-gcp-home-project-creds: {{ include "harnesscommon.secrets.passwords.manage" (dict "secret" "batch-processing-secret-mount" "key" "ce-gcp-home-project-creds" "providedValues" (list "ceGCPHomeProjectCreds") "length" 10 "context" $) }}
    {{- end }}
    cloud-data-store: {{ include "harnesscommon.secrets.passwords.manage" (dict "secret" "batch-processing-secret-mount" "key" "cloud-data-store" "providedValues" (list "storageObjectAdmin") "length" 10 "context" $) }}
{{- end }}

{{- define "batch-processing.pullSecrets" -}}
{{- if .Values.waitForInitContainer }}
    {{ include "common.images.pullSecrets" (dict "images" (list .Values.image .Values.waitForInitContainer.image) "global" .Values.global ) }}
{{- else }}
    {{ include "common.images.pullSecrets" (dict "images" (list .Values.image .Values.global.waitForInitContainer.image) "global" .Values.global ) }}
{{- end }}
{{- end -}}