package ru.beeline.fdmbpm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.beeline.fdmbpm.client.CapabilityClient;
import ru.beeline.fdmbpm.client.DashboardClient;
import ru.beeline.fdmbpm.client.DocumentServiceClient;
import ru.beeline.fdmbpm.client.PackageClient;
import ru.beeline.fdmbpm.dto.PackageRegistrationResponseDTO;
import ru.beeline.fdmbpm.dto.importExcel.ExcelBcDTO;
import ru.beeline.fdmbpm.dto.importExcel.ExcelDTO;
import ru.beeline.fdmbpm.dto.importExcel.ExcelTcDTO;
import ru.beeline.fdmbpm.exception.NotFoundException;
import ru.beeline.fdmbpm.exception.S3Exception;
import ru.beeline.fdmbpm.exception.ValidationException;
import ru.beeline.fdmbpm.mapper.ExcelBCMapper;
import ru.beeline.fdmbpm.mapper.ExcelTcMapper;
import ru.beeline.fdmlib.dto.capability.BusinessCapabilityDTO;
import ru.beeline.fdmlib.dto.capability.TechCapabilityShortDTO;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ImportProcessService {

    @Value("${queue.package-queue.name}")
    private String packageQueueName;

    @Autowired
    DocumentServiceClient documentServiceClient;

    @Autowired
    PackageClient packageClient;

    @Autowired
    CapabilityClient capabilityClient;

    @Autowired
    DashboardClient dashboardClient;

    @Autowired
    CapabilityService capabilityService;

    @Autowired
    ExcelBCMapper excelBCMapper;

    @Autowired
    ExcelTcMapper excelTcMapper;

    private ObjectMapper objectMapper = new ObjectMapper();

    public Integer uploadingDataFromExcel(String entityType, Boolean sync, Integer docId) {
        log.info("entityType: {} , sync: {} , docId: {} .", entityType, sync, docId);
        ResponseEntity<byte[]> response = getDocumentWithRetry(docId);
        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("Document retrieved successfully with status: {}", response.getStatusCode());
            byte[] fileData = response.getBody();
            String fileName = response.getHeaders().getContentDisposition().getFilename();
            Set<String> expectedColumns = new HashSet<>(Arrays.asList("code", "name", "description", "parents",
                    "isdomain", "status", "author", "link", "owner"));
            String operation = getOperationType(entityType);
            if (fileValidation(fileName) && columnValidation(fileData, expectedColumns)) {
                log.info("File and columns are valid.");
                List<ExcelDTO> excelDTOS = convertExcelToJson(fileData);
                if (entityType.equals("business_capability")) {
                    List<ExcelBcDTO> excelBcDTOS = sort(excelBCMapper.convert(excelDTOS));
                    if (sync) {
                        purgingBusinessCapability(excelBcDTOS);
                    }
                    return registerAndSendBusinessCapabilityPackage(operation, excelBcDTOS);
                } else if (entityType.equals("tech_capability")) {
                    List<ExcelTcDTO> excelTcDTOS = excelTcMapper.convert(excelDTOS);
                    if (sync) {
                        purgingTechCapability(excelTcDTOS);
                    }
                    return registerAndSendTechCapabilityPackage(operation, excelTcDTOS);
                }
            } else {
                log.error("Invalid file format: {}", fileName);
                PackageRegistrationResponseDTO responseDTO = packageClient.registerPackage(operation,
                        1, "excel", "VALIDATE ERROR");
                if (responseDTO == null) {
                    throw new ValidationException("Not valid file, not registered");
                }
                return responseDTO.getPackageId();
            }
        } else {
            handleErrorResponse(response, docId);
        }
        return null;
    }

    private ResponseEntity<byte[]> getDocumentWithRetry(Integer docId) {
        ResponseEntity<byte[]> response = documentServiceClient.getDocument(docId);
        int count = 0;
        int time = 5000;
        while (response.getStatusCode().isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE) && count < 3) {
            try {
                Thread.sleep(time);
                response = documentServiceClient.getDocument(docId);
                count++;
                time *= 2;
            } catch (InterruptedException e) {
                log.error("InterruptedException occurred: ", e);
                Thread.currentThread().interrupt();
            }
        }
        return response;
    }

    private Integer registerAndSendBusinessCapabilityPackage(String operation, List<ExcelBcDTO> excelBcDTOS) {
        try {
            log.info("Register package, operation: {} , excelBcDTOS size: {}", operation, excelBcDTOS.size());
            PackageRegistrationResponseDTO responseDTO = packageClient.registerPackage(operation, excelBcDTOS.size(),"excel");
            log.info("packageId: {}", responseDTO.getPackageId());
            ObjectNode messagePayload = createMessagePayloadForBc(responseDTO, excelBcDTOS);
            log.info("Send to package-queue");
            capabilityService.sendMessageToCapabilityQueue(packageQueueName, objectMapper.writeValueAsString(messagePayload));
            log.info("method completed");
            return responseDTO.getPackageId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Integer registerAndSendTechCapabilityPackage(String operation, List<ExcelTcDTO> excelTcDTOS) {
        try {
            log.info("Register package, operation: {} , excelTcDTOS size: {}", operation, excelTcDTOS.size());
            PackageRegistrationResponseDTO responseDTO = packageClient.registerPackage(operation, excelTcDTOS.size(),"excel");
            log.info("packageId: {}", responseDTO.getPackageId());
            ObjectNode messagePayload = createMessagePayloadForTc(responseDTO, excelTcDTOS);
            log.info("Send to package-queue");
            capabilityService.sendMessageToCapabilityQueue(packageQueueName, objectMapper.writeValueAsString(messagePayload));
            log.info("method completed");
            return responseDTO.getPackageId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getOperationType(String entityType) {
        if (entityType.equals("business_capability")) {
            return "UPDATE_BUSINESS_CAPABILITY_FROM_EXCEL";
        } else if (entityType.equals("tech_capability")) {
            return "UPDATE_TECH_CAPABILITY_FROM_EXCEL";
        } else {
            throw new ValidationException("Entity type not valid.");
        }
    }

    private ObjectNode createMessagePayloadForBc(PackageRegistrationResponseDTO responseDTO, List<ExcelBcDTO> excelBcDTOS) {
        ObjectNode messagePayload = objectMapper.createObjectNode();
        messagePayload.put("packageId", responseDTO.getPackageId());
        ArrayNode payloadArray = messagePayload.putArray("payload");
        excelBcDTOS.forEach(bcDTO -> {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("code", bcDTO.getCode());
            item.put("name", bcDTO.getName());
            item.put("description", bcDTO.getDescription());
            item.put("status", bcDTO.getStatus());
            item.put("author", bcDTO.getAuthor());
            item.put("link", bcDTO.getLink());
            item.put("owner", bcDTO.getOwner());
            item.putPOJO("parent", bcDTO.getParent());
            item.put("isDomain", bcDTO.getIsDomain());
            payloadArray.add(item);
        });
        return messagePayload;
    }

    private ObjectNode createMessagePayloadForTc(PackageRegistrationResponseDTO responseDTO, List<ExcelTcDTO> excelTcDTOS) {
        ObjectNode messagePayload = objectMapper.createObjectNode();
        messagePayload.put("packageId", responseDTO.getPackageId());
        ArrayNode payloadArray = messagePayload.putArray("payload");
        excelTcDTOS.forEach(tcDTO -> {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("code", tcDTO.getCode());
            item.put("name", tcDTO.getName());
            item.put("description", tcDTO.getDescription());
            item.put("status", tcDTO.getStatus());
            item.put("author", tcDTO.getAuthor());
            item.put("link", tcDTO.getLink());
            item.put("owner", tcDTO.getOwner());
            item.putPOJO("parents", tcDTO.getParents());
            payloadArray.add(item);
        });
        return messagePayload;
    }

    public void purgingBusinessCapability(List<ExcelBcDTO> excelBcDTOS) {
        log.info("purgingBusinessCapability");
        List<BusinessCapabilityDTO> businessCapabilityDTOS = capabilityClient.getBusinessCapabilities();
        List<BusinessCapabilityDTO> uniqueBusinessCapability = businessCapabilityDTOS.stream()
                .filter(businessCapability -> excelBcDTOS.stream().noneMatch(excelBc ->
                        excelBc.getCode().equals(businessCapability.getCode())))
                .toList();
        log.info("Unique Business Capability:" + uniqueBusinessCapability);
        capabilityClient.deleteBusinessCapabilities(uniqueBusinessCapability);
    }

    public void purgingTechCapability(List<ExcelTcDTO> excelTcDTOS) {
        log.info("purgingTechCapability");
        List<TechCapabilityShortDTO> techCapabilityDTOS = capabilityClient.getTechCapabilities();
        List<TechCapabilityShortDTO> uniqueTechCapability =
                techCapabilityDTOS.stream().filter(techCapability -> excelTcDTOS.stream().
                                noneMatch(excelTc -> excelTc.getCode().equals(techCapability.getCode())))
                        .toList();
        log.info("Unique Tech Capability:" + uniqueTechCapability);
        capabilityClient.deleteTechCapabilities(uniqueTechCapability);
    }

    private Boolean columnValidation(byte[] fileBytes, Set<String> expectedColumns) {
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes);
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Set<String> actualColumns = new HashSet<>();
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                    Cell cell = headerRow.getCell(j);
                    if (cell != null) {
                        actualColumns.add(normalizeColumnName(cell.getStringCellValue()));
                    }
                }
            }
            return actualColumns.containsAll(expectedColumns);
        } catch (IOException e) {
            log.error("Error reading file: ", e);
            return false;
        }
    }

    private Boolean fileValidation(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        int dotIndex = fileName.indexOf('.');
        if (dotIndex == -1) {
            return false;
        }
        String fileExtension = fileName.substring(dotIndex);
        List<String> validFormats = Arrays.asList(".xlsx", ".xls", ".xlsm", ".xlsb");
        for (String format : validFormats) {
            if (format.equalsIgnoreCase(fileExtension)) {
                return true;
            }
        }
        return false;
    }

    public List<ExcelDTO> convertExcelToJson(byte[] fileBytes) {
        List<ExcelDTO> excelDTOList = new ArrayList<>();
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            boolean hasIsDomain = false;

            for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                Cell headerCell = headerRow.getCell(j);
                if (normalizeColumnName(headerCell.getStringCellValue()).equals("isdomain")) {
                    hasIsDomain = true;
                    break;
                }
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                ExcelDTO excelDTO = new ExcelDTO();
                for (int j = 0; j < headerRow.getLastCellNum(); j++) {
                    Cell headerCell = headerRow.getCell(j);
                    Cell cell = row.getCell(j);
                    String columnName = normalizeColumnName(headerCell.getStringCellValue());
                    String cellValue = getCellValueAsString(cell);
                    switch (columnName) {
                        case "code":
                            excelDTO.setCode(cellValue);
                            break;
                        case "name":
                            excelDTO.setName(cellValue);
                            break;
                        case "description":
                            excelDTO.setDescription(cellValue);
                            break;
                        case "status":
                            excelDTO.setStatus(cellValue);
                            break;
                        case "author":
                            excelDTO.setAuthor(cellValue);
                            break;
                        case "link":
                            excelDTO.setLink(cellValue);
                            break;
                        case "owner":
                            excelDTO.setOwner(cellValue);
                            break;
                        case "parents":
                            List<String> parents = Arrays.asList(cellValue.split(","));
                            excelDTO.setParents(parents);
                            break;
                        case "isdomain":
                            excelDTO.setIsDomain(Boolean.parseBoolean(cellValue));
                            break;
                    }
                }
                if (!hasIsDomain) {
                    excelDTO.setIsDomain(null);
                }
                excelDTOList.add(excelDTO);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return excelDTOList;
    }

    private String normalizeColumnName(String columnName) {
        return columnName.toLowerCase().trim();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    public List<ExcelBcDTO> sort(List<ExcelBcDTO> excelBcDTOS) {
        List<ExcelBcDTO> sorted = new ArrayList<>();
        List<ExcelBcDTO> remaining = new ArrayList<>(excelBcDTOS);
        sorted.addAll(remaining.stream()
                .filter(e -> e.getParent() == null || e.getParent().isEmpty())
                .collect(Collectors.toList()));
        remaining.removeAll(sorted);
        int previousSize;
        do {
            previousSize = remaining.size();
            for (ExcelBcDTO excelBcDTO : new ArrayList<>(remaining)) {
                if (excelBcDTO.getParent() != null && !excelBcDTO.getParent().isEmpty()
                        && sorted.stream().anyMatch(e -> excelBcDTO.getParent().equals(e.getCode()))) {
                    sorted.add(excelBcDTO);
                    remaining.remove(excelBcDTO);
                }
            }
        } while (previousSize != remaining.size() && !remaining.isEmpty());

        if (!remaining.isEmpty()) {
            throw new IllegalArgumentException("Для объектов: " + remaining.stream()
                    .map(ExcelBcDTO::getCode)
                    .collect(Collectors.joining(", ")) + " - не существует указанных родителей");
        }
        return sorted;
    }

    private void handleErrorResponse(ResponseEntity<byte[]> response, Integer docId) {
        if (response.getStatusCode().is4xxClientError()) {
            log.error("Client error occurred with status: {}", response.getStatusCode());
            throw new NotFoundException(String.format("The record with this id: %s was not found", docId));
        } else if (response.getStatusCode().is5xxServerError()) {
            log.error("Server error occurred with status: {}", response.getStatusCode());
            throw new S3Exception("Ошибка при загрузке документа: " + response.getStatusCode());
        } else {
            log.error("Unexpected error occurred with status: {}", response.getStatusCode());
            throw new RuntimeException("Unexpected error occurred with status: " + response.getStatusCode());
        }
    }
}
