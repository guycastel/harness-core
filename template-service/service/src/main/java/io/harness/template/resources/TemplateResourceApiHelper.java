/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.template.resources;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ng.core.template.TemplateEntityConstants.ALL;
import static io.harness.ng.core.template.TemplateEntityConstants.LAST_UPDATES_TEMPLATE;
import static io.harness.ng.core.template.TemplateEntityConstants.STABLE_TEMPLATE;

import static java.lang.Long.parseLong;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitaware.helper.GitAwareContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.ng.core.template.TemplateEntityType;
import io.harness.ng.core.template.TemplateListType;
import io.harness.ng.core.template.TemplateMergeResponseDTO;
import io.harness.ng.core.template.TemplateMetadataSummaryResponseDTO;
import io.harness.ng.core.template.TemplateResponseDTO;
import io.harness.ng.core.template.TemplateWithInputsResponseDTO;
import io.harness.pms.yaml.YamlUtils;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.spec.server.template.v1.model.FixedValueFieldDependencyDetailsDTO;
import io.harness.spec.server.template.v1.model.GitCreateDetails;
import io.harness.spec.server.template.v1.model.GitFindDetails;
import io.harness.spec.server.template.v1.model.GitImportDetails;
import io.harness.spec.server.template.v1.model.GitUpdateDetails;
import io.harness.spec.server.template.v1.model.InputDetailsDTO;
import io.harness.spec.server.template.v1.model.RuntimeInputDependencyDetailsDTO;
import io.harness.spec.server.template.v1.model.TemplateCreateRequestBody;
import io.harness.spec.server.template.v1.model.TemplateImportRequestDTO;
import io.harness.spec.server.template.v1.model.TemplateImportResponseBody;
import io.harness.spec.server.template.v1.model.TemplateInputSchemaDetailsResponseBody;
import io.harness.spec.server.template.v1.model.TemplateMetadataSummaryResponse;
import io.harness.spec.server.template.v1.model.TemplateUpdateGitMetadataRequest;
import io.harness.spec.server.template.v1.model.TemplateUpdateRequestBody;
import io.harness.spec.server.template.v1.model.TemplateUpdateStableResponse;
import io.harness.spec.server.template.v1.model.TemplateYamlInputDTO;
import io.harness.spec.server.template.v1.model.TemplateYamlInputDetailsDTO;
import io.harness.spec.server.template.v1.model.TemplateYamlInputMetadataDTO;
import io.harness.spec.server.template.v1.model.YamlInputDependencyDetailsDTO;
import io.harness.spec.server.template.v1.model.YamlInputType;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.mappers.NGTemplateDtoMapper;
import io.harness.template.resources.beans.FilterParamsDTO;
import io.harness.template.resources.beans.PageParamsDTO;
import io.harness.template.resources.beans.PermissionTypes;
import io.harness.template.resources.beans.TemplateFilterProperties;
import io.harness.template.resources.beans.TemplateFilterPropertiesDTO;
import io.harness.template.resources.beans.TemplateUpdateGitMetadataResponse;
import io.harness.template.resources.beans.UpdateGitDetailsList;
import io.harness.template.resources.beans.UpdateGitDetailsParams;
import io.harness.template.services.NGTemplateSchemaService;
import io.harness.template.services.NGTemplateService;
import io.harness.template.services.TemplateMergeService;
import io.harness.utils.ApiUtils;
import io.harness.yaml.schema.inputs.beans.DependencyDetails;
import io.harness.yaml.schema.inputs.beans.InputDetails;
import io.harness.yaml.schema.inputs.beans.InputMetadata;
import io.harness.yaml.schema.inputs.beans.SchemaInputType;
import io.harness.yaml.schema.inputs.beans.YamlInputDetails;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(CDC)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@NextGenManagerAuth
public class TemplateResourceApiHelper {
  public static final String TEMPLATE = "TEMPLATE";

  private final NGTemplateService templateService;
  private final AccessControlClient accessControlClient;
  private final TemplateResourceApiMapper templateResourceApiMapper;
  private final TemplateMergeService templateMergeService;
  private final NGTemplateSchemaService ngTemplateSchemaService;

  public Response getTemplate(@AccountIdentifier String account, @OrgIdentifier String org,
      @ProjectIdentifier String project, @ResourceIdentifier String templateIdentifier, String versionLabel,
      boolean deleted, String branch, String parentConnectorRef, String parentRepoName, String parentAccountId,
      String parentOrgId, String parentProjectId, Boolean getInputYaml) {
    // if label is not given, return stable template
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(account, org, project),
        Resource.of(TEMPLATE, templateIdentifier), PermissionTypes.TEMPLATE_VIEW_PERMISSION);
    GitAwareContextHelper.populateGitDetails(GitEntityInfo.builder()
                                                 .branch(branch)
                                                 .parentEntityConnectorRef(parentConnectorRef)
                                                 .parentEntityRepoName(parentRepoName)
                                                 .parentEntityAccountIdentifier(parentAccountId)
                                                 .parentEntityOrgIdentifier(parentOrgId)
                                                 .parentEntityProjectIdentifier(parentProjectId)
                                                 .build());
    if (String.valueOf(getInputYaml).equals("null")) {
      getInputYaml = false;
    }

    if (getInputYaml) {
      // returns template along with templateInputs yaml
      log.info(String.format(
          "Gets Template along with Template inputs for template with identifier %s in project %s, org %s, account %s",
          templateIdentifier, project, org, account));
      TemplateWithInputsResponseDTO templateWithInputs =
          templateService.getTemplateWithInputs(account, org, project, templateIdentifier, versionLabel, false);
      String version = "0";
      if (templateWithInputs != null && templateWithInputs.getTemplateResponseDTO() != null
          && templateWithInputs.getTemplateResponseDTO().getVersion() != null) {
        version = String.valueOf(templateWithInputs.getTemplateResponseDTO().getVersion());
      }
      return Response.ok()
          .entity(templateResourceApiMapper.toTemplateWithInputResponse(templateWithInputs))
          .tag(version)
          .build();
    } else {
      log.info(
          String.format("Retrieving Template with identifier %s and versionLabel %s in project %s, org %s, account %s",
              templateIdentifier, versionLabel, project, org, account));
      Optional<TemplateEntity> templateEntity =
          templateService.get(account, org, project, templateIdentifier, versionLabel, deleted, false);

      String version = "0";
      if (templateEntity.isPresent()) {
        version = templateEntity.get().getVersion().toString();
      }
      TemplateResponseDTO templateResponseDTO = NGTemplateDtoMapper.writeTemplateResponseDto(templateEntity.orElseThrow(
          ()
              -> new NotFoundException(String.format(
                  "Template with the given Identifier: %s and %s does not exist or has been deleted",
                  templateIdentifier,
                  EmptyPredicate.isEmpty(versionLabel) ? "stable versionLabel" : "versionLabel: " + versionLabel))));
      return Response.ok()
          .entity(templateResourceApiMapper.toTemplateResponseDefault(templateResponseDTO))
          .tag(version)
          .build();
    }
  }

  public static TemplateInputSchemaDetailsResponseBody getTemplateInputSchemaDetailsResponseBody(
      List<YamlInputDetails> yamlInputDetailsList) {
    TemplateInputSchemaDetailsResponseBody responseBody = new TemplateInputSchemaDetailsResponseBody();
    List<TemplateYamlInputDetailsDTO> templateYamlInputDetailsDTOList = new ArrayList<>();
    yamlInputDetailsList.forEach(
        yamlInputDetails -> templateYamlInputDetailsDTOList.add(toTemplateYamlInputDetailsDTO(yamlInputDetails)));

    responseBody.setInputs(templateYamlInputDetailsDTOList);
    return responseBody;
  }

  private static TemplateYamlInputDetailsDTO toTemplateYamlInputDetailsDTO(YamlInputDetails yamlInputDetails) {
    TemplateYamlInputDetailsDTO templateYamlInputDetailsDTO = new TemplateYamlInputDetailsDTO();
    templateYamlInputDetailsDTO.setDetails(toTemplateYamlInputDTO(yamlInputDetails.getInputDetails()));
    if (yamlInputDetails.getInputMetadataWrapper() != null
        && isNotEmpty(yamlInputDetails.getInputMetadataWrapper().getInputMetadataList())) {
      templateYamlInputDetailsDTO.setMetadata(yamlInputDetails.getInputMetadataWrapper()
                                                  .getInputMetadataList()
                                                  .stream()
                                                  .map(TemplateResourceApiHelper::toTemplateYamlInputMetadataDTO)
                                                  .collect(Collectors.toList()));
    }
    return templateYamlInputDetailsDTO;
  }

  private static TemplateYamlInputDTO toTemplateYamlInputDTO(InputDetails inputDetails) {
    TemplateYamlInputDTO templateYamlInputDTO = new TemplateYamlInputDTO();
    templateYamlInputDTO.setName(inputDetails.getName());
    templateYamlInputDTO.setDescription(inputDetails.getDescription());
    templateYamlInputDTO.setType(getYamlInputType(inputDetails.getType()));
    templateYamlInputDTO.setRequired(inputDetails.isRequired());
    templateYamlInputDTO.setDefault(inputDetails.getDefaultValue());
    templateYamlInputDTO.setAllowedValues(inputDetails.getAllowedValues());
    templateYamlInputDTO.setRegex(inputDetails.getRegex());
    templateYamlInputDTO.execution(inputDetails.getExecution());
    return templateYamlInputDTO;
  }

  private static TemplateYamlInputMetadataDTO toTemplateYamlInputMetadataDTO(InputMetadata inputMetadata) {
    TemplateYamlInputMetadataDTO templateYamlInputMetadataDTO = new TemplateYamlInputMetadataDTO();
    if (inputMetadata != null) {
      templateYamlInputMetadataDTO.setFieldProperties(toInputDetailsDTOList(inputMetadata.getInputDetails()));
      templateYamlInputMetadataDTO.setDependencies(
          toYamlInputDependencyDetailsDTO(inputMetadata.getDependencyDetails()));
    }
    return templateYamlInputMetadataDTO;
  }

  private static InputDetailsDTO toInputDetailsDTOList(InputMetadata.InputDetails inputDetails) {
    // TODO: Populate entityGroup, entityType, path, stageType
    InputDetailsDTO inputDetailsPerFieldDTO = new InputDetailsDTO();
    inputDetailsPerFieldDTO.setInputType(inputDetails.getInputType());
    return inputDetailsPerFieldDTO;
  }

  private static YamlInputDependencyDetailsDTO toYamlInputDependencyDetailsDTO(DependencyDetails dependencyDetails) {
    YamlInputDependencyDetailsDTO yamlInputDependencyDetailsDTO = new YamlInputDependencyDetailsDTO();
    List<FixedValueFieldDependencyDetailsDTO> fixedValueFieldDependencyDetailsDTOList = new ArrayList<>();
    List<RuntimeInputDependencyDetailsDTO> runtimeInputDependencyDetailsDTOList = new ArrayList<>();

    if (dependencyDetails != null) {
      if (dependencyDetails.getRuntimeInputDependencyDetailsList() != null) {
        dependencyDetails.getRuntimeInputDependencyDetailsList().forEach(inputDependencyDetails -> {
          RuntimeInputDependencyDetailsDTO runtimeInputDependencyDetailsDTO = new RuntimeInputDependencyDetailsDTO();
          runtimeInputDependencyDetailsDTO.setInputName(inputDependencyDetails.getInputName());
          runtimeInputDependencyDetailsDTO.setFieldName(inputDependencyDetails.getFieldName());
          runtimeInputDependencyDetailsDTOList.add(runtimeInputDependencyDetailsDTO);
        });
      }
      if (dependencyDetails.getFixedValueDependencyDetailsList() != null) {
        dependencyDetails.getFixedValueDependencyDetailsList().forEach(constantDependencyDetails -> {
          FixedValueFieldDependencyDetailsDTO fixedValueFieldDependencyDetailsDTO =
              new FixedValueFieldDependencyDetailsDTO();
          fixedValueFieldDependencyDetailsDTO.setFieldName(constantDependencyDetails.getPropertyName());
          fixedValueFieldDependencyDetailsDTO.setFieldValue(constantDependencyDetails.getFieldValue());
          fixedValueFieldDependencyDetailsDTO.setFieldInputType(constantDependencyDetails.getPropertyType());
          fixedValueFieldDependencyDetailsDTOList.add(fixedValueFieldDependencyDetailsDTO);
        });
      }
    }

    yamlInputDependencyDetailsDTO.setRequiredFixedValues(fixedValueFieldDependencyDetailsDTOList);
    yamlInputDependencyDetailsDTO.setRequiredRuntimeInputs(runtimeInputDependencyDetailsDTOList);
    return yamlInputDependencyDetailsDTO;
  }

  private static YamlInputType getYamlInputType(SchemaInputType schemaInputType) {
    switch (schemaInputType) {
      case STRING:
        return YamlInputType.STRING;
      case BOOLEAN:
        return YamlInputType.BOOLEAN;
      case INTEGER:
        return YamlInputType.INTEGER;
      default:
        return YamlInputType.OBJECT;
    }
  }

  public Response createTemplate(@AccountIdentifier String account, @OrgIdentifier String org,
      @ProjectIdentifier String project, GitCreateDetails gitEntityCreateInfo, TemplateRequestInfoDTO requestInfoDTO,
      boolean setDefaultTemplate, String comments) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(account, org, project), Resource.of(TEMPLATE, null), PermissionTypes.TEMPLATE_EDIT_PERMISSION);
    GitAwareContextHelper.populateGitDetails(templateResourceApiMapper.populateGitCreateDetails(gitEntityCreateInfo));
    TemplateEntity templateEntity = NGTemplateDtoMapper.toTemplateEntity(account, org, project, requestInfoDTO);
    log.info(String.format("Creating Template with identifier %s with label %s in project %s, org %s, account %s",
        templateEntity.getIdentifier(), templateEntity.getVersionLabel(), project, org, account));
    TemplateEntity createdTemplate = templateService.create(templateEntity, setDefaultTemplate, comments, false);
    TemplateResponseDTO templateResponseDTO = NGTemplateDtoMapper.writeTemplateResponseDto(createdTemplate);
    return Response.status(Response.Status.CREATED)
        .entity(templateResourceApiMapper.toTemplateResponse(templateResponseDTO))
        .tag(createdTemplate.getVersion().toString())
        .build();
  }

  public Response importTemplate(@AccountIdentifier String account, @OrgIdentifier String org,
      @ProjectIdentifier String project, @ResourceIdentifier String templateIdentifier,
      GitImportDetails gitImportDetails, TemplateImportRequestDTO templateImportRequest) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(account, org, project), Resource.of(TEMPLATE, null), PermissionTypes.TEMPLATE_EDIT_PERMISSION);

    io.harness.template.resources.beans.TemplateImportRequestDTO templateImportRequestDTO =
        io.harness.template.resources.beans.TemplateImportRequestDTO.builder()
            .templateVersion(templateImportRequest.getTemplateVersion())
            .templateDescription(templateImportRequest.getTemplateVersion())
            .templateName(templateImportRequest.getTemplateName())
            .build();
    GitAwareContextHelper.populateGitDetails(GitEntityInfo.builder()
                                                 .branch(gitImportDetails.getBranchName())
                                                 .connectorRef(gitImportDetails.getConnectorRef())
                                                 .filePath(gitImportDetails.getFilePath())
                                                 .repoName(gitImportDetails.getRepoName())
                                                 .build());
    log.info(String.format("Importing Template with name %s with version %s in project %s, org %s, account %s",
        templateImportRequestDTO.getTemplateName(), templateImportRequestDTO.getTemplateVersion(), project, org,
        account));
    TemplateEntity importedTemplate = templateService.importTemplateFromRemote(account, org, project,
        templateIdentifier, templateImportRequestDTO, Boolean.TRUE.equals(gitImportDetails.isIsForceImport()));
    TemplateImportResponseBody responseBody = new TemplateImportResponseBody();
    responseBody.setTemplateIdentifier(importedTemplate.getIdentifier());
    responseBody.setTemplateVersion(importedTemplate.getVersionLabel());
    return Response.ok().entity(responseBody).build();
  }

  public Response updateGitMetaData(@AccountIdentifier String account, @OrgIdentifier String org,
      @ProjectIdentifier String project, @ResourceIdentifier String templateIdentifier,
      List<TemplateUpdateGitMetadataRequest> templateUpdateGitMetadataRequests) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(account, org, project), Resource.of(TEMPLATE, null), PermissionTypes.TEMPLATE_EDIT_PERMISSION);
    List<UpdateGitDetailsList> updateGitDetailsLists;
    try {
      updateGitDetailsLists =
          templateUpdateGitMetadataRequests.stream()
              .map(request
                  -> UpdateGitDetailsList.builder()
                         .updateGitDetailsParams(UpdateGitDetailsParams.builder()
                                                     .connectorRef(request.getGitDetails().getConnectorRef())
                                                     .repoName(request.getGitDetails().getRepoName())
                                                     .filePath(request.getGitDetails().getFilePath())
                                                     .build())
                         .version(request.getVersion())
                         .build())
              .collect(Collectors.toList());
    } catch (Exception exception) {
      throw new InvalidRequestException("Please provide valid Git details");
    }

    templateService.updateGitDetailsForMultipleVersion(
        account, org, project, templateIdentifier, updateGitDetailsLists);
    return Response.ok().entity(TemplateUpdateGitMetadataResponse.builder().status(true).build()).build();
  }

  public Response updateTemplate(@AccountIdentifier String account, @OrgIdentifier String org,
      @ProjectIdentifier String project, @ResourceIdentifier String templateIdentifier, String versionLabel,
      GitUpdateDetails gitEntityInfo, TemplateRequestInfoDTO requestInfoDTO, boolean setDefaultTemplate,
      String comments) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(account, org, project),
        Resource.of(TEMPLATE, templateIdentifier), PermissionTypes.TEMPLATE_EDIT_PERMISSION);
    GitAwareContextHelper.populateGitDetails(templateResourceApiMapper.populateGitUpdateDetails(gitEntityInfo));
    TemplateEntity templateEntity =
        NGTemplateDtoMapper.toTemplateEntity(account, org, project, templateIdentifier, versionLabel, requestInfoDTO);
    log.info(
        String.format("Updating Template with identifier %s with versionLabel %s in project %s, org %s, account %s",
            templateEntity.getIdentifier(), templateEntity.getVersionLabel(), project, org, account));
    templateEntity = templateEntity.withVersion(isNumeric("ifMatch") ? parseLong("ifMatch") : null);
    TemplateEntity updatedTemplate =
        templateService.updateTemplateEntity(templateEntity, ChangeType.MODIFY, setDefaultTemplate, comments);
    TemplateResponseDTO templateResponseDTO = NGTemplateDtoMapper.writeTemplateResponseDto(updatedTemplate);
    return Response.ok()
        .entity(templateResourceApiMapper.toTemplateResponse(templateResponseDTO))
        .tag(updatedTemplate.getVersion().toString())
        .build();
  }

  public Response updateStableTemplate(@AccountIdentifier String account, @OrgIdentifier String org,
      @ProjectIdentifier String project, @ResourceIdentifier String templateIdentifier, String versionLabel,
      GitFindDetails gitEntityBasicInfo, String comments) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(account, org, project),
        Resource.of(TEMPLATE, templateIdentifier), PermissionTypes.TEMPLATE_EDIT_PERMISSION);
    GitAwareContextHelper.populateGitDetails(templateResourceApiMapper.populateGitFindDetails(gitEntityBasicInfo));
    log.info(String.format(
        "Updating Stable Template with identifier %s with versionLabel %s in project %s, org %s, account %s",
        templateIdentifier, versionLabel, project, org, account));

    TemplateEntity templateEntity =
        templateService.updateStableTemplateVersion(account, org, project, templateIdentifier, versionLabel, comments);
    TemplateUpdateStableResponse templateUpdateStableResponse = new TemplateUpdateStableResponse();
    templateUpdateStableResponse.setStableVersion(templateEntity.getVersionLabel());
    return Response.ok().entity(templateUpdateStableResponse).tag(templateEntity.getVersion().toString()).build();
  }

  public Response deleteTemplate(@AccountIdentifier String account, @OrgIdentifier String org,
      @ProjectIdentifier String project, @ResourceIdentifier String templateIdentifier, String versionLabel,
      String comments, boolean forceDelete) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(account, org, project),
        Resource.of(TEMPLATE, templateIdentifier), PermissionTypes.TEMPLATE_DELETE_PERMISSION);
    log.info(String.format("Deleting Template with identifier %s and versionLabel %s in project %s, org %s, account %s",
        templateIdentifier, versionLabel, project, org, account));

    templateService.delete(account, org, project, templateIdentifier, versionLabel,
        isNumeric("ifMatch") ? parseLong("ifMatch") : null, comments, forceDelete);
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  public Response getInputsSchema(@AccountIdentifier String account, @OrgIdentifier String org,
      @ProjectIdentifier String project, @ResourceIdentifier String templateIdentifier, String versionLabel) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(account, org, project),
        Resource.of(TEMPLATE, templateIdentifier), PermissionTypes.TEMPLATE_VIEW_PERMISSION);
    Optional<TemplateEntity> templateEntity =
        templateService.get(account, org, project, templateIdentifier, versionLabel, false, false);
    if (!templateEntity.isPresent()) {
      throw new NotFoundException(String.format(
          "Template with the given Identifier: %s and %s does not exist or has been deleted", templateIdentifier,
          EmptyPredicate.isEmpty(versionLabel) ? "stable versionLabel" : "versionLabel: " + versionLabel));
    }
    TemplateMergeResponseDTO templateMergeResponseDTO = templateMergeService.applyTemplatesToYamlV2(account, org,
        project, YamlUtils.readAsJsonNode(templateEntity.get().getYaml()), false, false, false,
        templateEntity.get().getHarnessVersion());
    List<YamlInputDetails> yamlInputDetails =
        ngTemplateSchemaService.getInputSchemaDetails(templateMergeResponseDTO.getMergedPipelineYaml());
    TemplateInputSchemaDetailsResponseBody responseBody =
        TemplateResourceApiHelper.getTemplateInputSchemaDetailsResponseBody(yamlInputDetails);
    return Response.ok().entity(responseBody).build();
  }

  public Response getTemplates(@AccountIdentifier String account, @OrgIdentifier String org,
      @ProjectIdentifier String project, int page, int limit, String sort, String order, String searchTerm,
      String listType, boolean recursive, List<String> names, List<String> identifiers, String description,
      List<String> entityTypes, List<String> childTypes) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(account, org, project), Resource.of(TEMPLATE, null), PermissionTypes.TEMPLATE_VIEW_PERMISSION);
    log.info(String.format("Get List of templates in project: %s, org: %s, account: %s", project, org, account));
    TemplateFilterPropertiesDTO filterProperties = new TemplateFilterPropertiesDTO();
    List<TemplateEntityType> templateEntityTypes =
        entityTypes.stream().map(TemplateEntityType::getTemplateType).collect(Collectors.toList());
    filterProperties.setTemplateEntityTypes(templateEntityTypes);
    filterProperties.setTemplateNames(names);
    filterProperties.setDescription(description);
    filterProperties.setTemplateIdentifiers(identifiers);
    filterProperties.setChildTypes(childTypes);
    TemplateFilterProperties templateFilterProperties =
        NGTemplateDtoMapper.toTemplateFilterProperties(filterProperties);
    String type = toListType(listType);
    TemplateListType templateListType = TemplateListType.getTemplateType(type);
    FilterParamsDTO filterParamsDTO = NGTemplateDtoMapper.prepareFilterParamsDTO(
        searchTerm, "", templateListType, templateFilterProperties, recursive, false);

    String sortQuery = templateResourceApiMapper.mapSort(sort, order);
    PageParamsDTO pageParamsDTO =
        NGTemplateDtoMapper.preparePageParamsDTO(page, limit, Collections.singletonList(sortQuery));

    Page<TemplateMetadataSummaryResponseDTO> templateMetadataSummaryResponseDTOS =
        templateService.listTemplateMetadata(account, org, project, filterParamsDTO, pageParamsDTO)
            .map(NGTemplateDtoMapper::prepareTemplateMetaDataSummaryResponseDto);

    Page<TemplateMetadataSummaryResponse> templateMetadataSummaryResponses =
        templateMetadataSummaryResponseDTOS.map(templateResourceApiMapper::mapToTemplateMetadataResponse);
    List<TemplateMetadataSummaryResponse> templateList = templateMetadataSummaryResponses.getContent();
    ResponseBuilder responseBuilder = Response.ok();
    ResponseBuilder responseBuilderWithLinks =
        ApiUtils.addLinksHeader(responseBuilder, templateMetadataSummaryResponses.getTotalElements(), page, limit);
    return responseBuilderWithLinks.entity(templateList).build();
  }
  public String toListType(String listType) {
    String type;
    if (isEmpty(listType)) {
      listType = "ALL";
    }
    switch (listType) {
      case "LAST_UPDATES_TEMPLATE":
        type = LAST_UPDATES_TEMPLATE;
        break;
      case "STABLE_TEMPLATE":
        type = STABLE_TEMPLATE;
        break;
      case "ALL":
        type = ALL;
        break;
      default:
        throw new InvalidRequestException(String.format(
            "Expected query param 'type' to be of value LAST_UPDATES_TEMPLATE, STABLE_TEMPLATE, ALL. [%s] value Not allowed",
            listType));
    }
    return type;
  }

  public TemplateRequestInfoDTO mapCreateToRequestInfoDTO(TemplateCreateRequestBody requestBody) {
    return TemplateRequestInfoDTO.builder()
        .label(requestBody.getLabel())
        .name(requestBody.getName())
        .identifier(requestBody.getIdentifier())
        .yaml(requestBody.getTemplateYaml())
        .description(requestBody.getDescription())
        .tags(requestBody.getTags())
        .build();
  }

  public TemplateRequestInfoDTO mapUpdateToRequestInfoDTO(TemplateUpdateRequestBody requestBody) {
    return TemplateRequestInfoDTO.builder()
        .label(requestBody.getLabel())
        .name(requestBody.getName())
        .identifier(requestBody.getIdentifier())
        .yaml(requestBody.getTemplateYaml())
        .description(requestBody.getDescription())
        .tags(requestBody.getTags())
        .build();
  }
}
