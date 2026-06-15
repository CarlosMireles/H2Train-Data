package com.h2traindata.dataapp.dataset.controller;

import com.h2traindata.dataapp.dataset.dto.DatasetCapabilitiesResponse;
import com.h2traindata.dataapp.dataset.dto.DatasetExportRequest;
import com.h2traindata.dataapp.dataset.dto.DatasetQueryRequest;
import com.h2traindata.dataapp.dataset.dto.HeartRateZoneDatasetRequest;
import com.h2traindata.dataapp.dataset.format.DatasetRenderedResponse;
import com.h2traindata.dataapp.dataset.format.DatasetRenderingService;
import com.h2traindata.dataapp.dataset.service.DatasetCapabilitiesService;
import com.h2traindata.dataapp.dataset.service.DatasetExportService;
import com.h2traindata.dataapp.dataset.service.DatasetQueryService;
import com.h2traindata.dataapp.dataset.service.HeartRateZoneDatasetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/datasets")
@Tag(
        name = "Datasets",
        description = "Datasets analíticos bajo demanda construidos desde los datamarts longitudinales"
)
public class DatasetQueryController {

    private final DatasetQueryService queryService;
    private final DatasetExportService exportService;
    private final DatasetCapabilitiesService capabilitiesService;
    private final DatasetRenderingService renderingService;
    private final HeartRateZoneDatasetService heartRateZoneService;

    public DatasetQueryController(DatasetQueryService queryService,
                                  DatasetExportService exportService,
                                  DatasetCapabilitiesService capabilitiesService,
                                  DatasetRenderingService renderingService,
                                  HeartRateZoneDatasetService heartRateZoneService) {
        this.queryService = queryService;
        this.exportService = exportService;
        this.capabilitiesService = capabilitiesService;
        this.renderingService = renderingService;
        this.heartRateZoneService = heartRateZoneService;
    }

    @GetMapping("/query")
    @Operation(
            summary = "Consultar sujetos mediante una métrica longitudinal agregada",
            description = "Agrega una métrica por sujeto dentro del rango de fechas opcional y devuelve "
                    + "los sujetos que cumplen la condición."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado de la consulta"),
            @ApiResponse(responseCode = "400", description = "Consulta no válida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Métrica no encontrada", content = @Content)
    })
    public ResponseEntity<byte[]> query(
            @Parameter(example = "daily_calories") @RequestParam String metric,
            @Parameter(example = "gt", description = "Valores admitidos: gt, gte, lt, lte, eq o between")
            @RequestParam String operator,
            @Parameter(example = "2500") @RequestParam BigDecimal value,
            @Parameter(example = "3000", description = "Obligatorio cuando operator=between")
            @RequestParam(required = false) BigDecimal maxValue,
            @Parameter(example = "avg", description = "Valores admitidos: avg, sum, min, max, count o latest")
            @RequestParam String aggregation,
            @Parameter(description = "Zonas cardiacas opcionales. Repetir el parámetro para seleccionar varias.")
            @RequestParam(required = false) List<String> zone,
            @Parameter(description = "Tipos de actividad opcionales. Repetir el parámetro para seleccionar varios.")
            @RequestParam(required = false) List<String> activityType,
            @Parameter(description = "Proveedores opcionales. Repetir el parámetro para seleccionar varios.")
            @RequestParam(required = false) List<String> provider,
            @Parameter(example = "2026-01-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(example = "2026-06-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(example = "json", description = "Valores admitidos: json, jsonl o csv")
            @RequestParam String format) {
        DatasetRenderedResponse rendered = renderingService.renderQuery(queryService.query(
                new DatasetQueryRequest(
                        metric,
                        operator,
                        value,
                        maxValue,
                        aggregation,
                        dimensions(zone, activityType, provider),
                        from,
                        to,
                        format
                )
        ));
        return response(rendered);
    }

    @GetMapping("/heart-rate-zones")
    @Operation(
            summary = "Construir un dataset analítico diario de zonas cardiacas",
            description = "Combina las series longitudinales de minutos y calorías por zona cardiaca, agrupadas "
                    + "por sujeto, fecha, proveedor y zona. También calcula minutos activos, minutos de alta "
                    + "intensidad, zona activa dominante y porcentajes temporales sin consultar el datalake "
                    + "de eventos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dataset diario de zonas cardiacas"),
            @ApiResponse(responseCode = "400", description = "Consulta no válida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Métrica de zona cardiaca no encontrada", content = @Content)
    })
    public ResponseEntity<byte[]> heartRateZones(
            @Parameter(example = "2026-01-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(example = "2026-06-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(
                    example = "cardio",
                    description = "Zonas normalizadas opcionales, como fat_burn, cardio o peak. "
                            + "Repetir el parámetro cuando sea necesario."
            )
            @RequestParam(required = false) List<String> zone,
            @Parameter(description = "Proveedores opcionales. Repetir el parámetro para seleccionar varios.")
            @RequestParam(required = false) List<String> provider,
            @Parameter(example = "json", description = "Valores admitidos: json, jsonl o csv")
            @RequestParam String format) {
        return response(renderingService.renderHeartRateZones(heartRateZoneService.query(
                new HeartRateZoneDatasetRequest(from, to, zone, provider, format)
        )));
    }

    @PostMapping(value = "/export", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Exportar un dataset longitudinal personalizado",
            description = "Selecciona sujetos mediante filtros agregados y exporta los puntos longitudinales "
                    + "de las métricas solicitadas en JSON, JSONL o CSV."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = DatasetExportRequest.class),
                    examples = @ExampleObject(
                            name = "Exportación con las métricas disponibles",
                            description = "La disponibilidad de métricas depende del datamart longitudinal. "
                                    + "Consultar GET /api/v1/datasets/query/capabilities antes de exportar.",
                            value = """
                            {
                              "metrics": ["daily_steps", "daily_calories"],
                              "filters": [{
                                "metric": "daily_calories",
                                "operator": "gt",
                                "value": 1400,
                                "aggregation": "avg"
                              }],
                              "dimensions": {
                                "provider": ["fitbit"]
                              },
                              "from": "2026-01-01",
                              "to": "2026-06-15",
                              "format": "csv"
                            }
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exportación del dataset personalizado"),
            @ApiResponse(responseCode = "400", description = "Solicitud de exportación no válida", content = @Content),
            @ApiResponse(responseCode = "404", description = "Métrica no encontrada", content = @Content)
    })
    public ResponseEntity<byte[]> export(@RequestBody DatasetExportRequest request) {
        return response(renderingService.renderExport(exportService.export(request)));
    }

    @GetMapping(value = "/query/capabilities", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Listar las capacidades de consulta de datasets",
            description = "Devuelve las métricas detectadas en el datamart longitudinal y todas las operaciones "
                    + "de consulta admitidas."
    )
    public DatasetCapabilitiesResponse capabilities() {
        return capabilitiesService.capabilities();
    }

    private Map<String, List<String>> dimensions(List<String> zones,
                                                  List<String> activityTypes,
                                                  List<String> providers) {
        Map<String, List<String>> dimensions = new LinkedHashMap<>();
        if (zones != null && !zones.isEmpty()) {
            dimensions.put("zone", zones);
        }
        if (activityTypes != null && !activityTypes.isEmpty()) {
            dimensions.put("activityType", activityTypes);
        }
        if (providers != null && !providers.isEmpty()) {
            dimensions.put("provider", providers);
        }
        return dimensions.isEmpty() ? null : Map.copyOf(dimensions);
    }

    private ResponseEntity<byte[]> response(DatasetRenderedResponse rendered) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(rendered.contentType()));
        if (rendered.filename() != null) {
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(rendered.filename())
                    .build());
        }
        return ResponseEntity.ok()
                .headers(headers)
                .body(rendered.body());
    }
}
