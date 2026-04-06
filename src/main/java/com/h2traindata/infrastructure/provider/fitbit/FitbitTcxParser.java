package com.h2traindata.infrastructure.provider.fitbit;

import static com.h2traindata.infrastructure.provider.common.PayloadSupport.put;

import java.io.StringReader;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

final class FitbitTcxParser {

    private FitbitTcxParser() {
    }

    static FitbitTcxData parse(String tcxXml) {
        if (!StringUtils.hasText(tcxXml)) {
            return FitbitTcxData.empty();
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(tcxXml)));
            Element activities = firstElement(document.getDocumentElement(), "Activities");
            Element activity = firstElement(activities, "Activity");
            if (activity == null) {
                return FitbitTcxData.empty();
            }

            String sport = activity.getAttribute("Sport");
            List<Map<String, Object>> laps = new ArrayList<>();
            List<Map<String, Object>> trackpoints = new ArrayList<>();
            Integer maxHeartRate = null;
            Double totalDistance = null;
            Double minAltitude = null;
            Double maxAltitude = null;
            Double startLatitude = null;
            Double startLongitude = null;
            Double endLatitude = null;
            Double endLongitude = null;

            for (Element lap : childElements(activity, "Lap")) {
                Map<String, Object> normalizedLap = new LinkedHashMap<>();
                put(normalizedLap, "startTime", parseInstant(lap.getAttribute("StartTime")));
                put(normalizedLap, "elapsedTimeSeconds", parseDouble(text(firstElement(lap, "TotalTimeSeconds"))));
                put(normalizedLap, "distanceMeters", parseDouble(text(firstElement(lap, "DistanceMeters"))));
                put(normalizedLap, "caloriesKcal", parseInteger(text(firstElement(lap, "Calories"))));
                if (!normalizedLap.isEmpty()) {
                    laps.add(normalizedLap);
                }

                Element track = firstElement(lap, "Track");
                if (track == null) {
                    continue;
                }

                for (Element trackpoint : childElements(track, "Trackpoint")) {
                    Map<String, Object> point = new LinkedHashMap<>();
                    Instant recordedAt = parseInstant(text(firstElement(trackpoint, "Time")));
                    Double latitude = parseDouble(text(firstElement(firstElement(trackpoint, "Position"), "LatitudeDegrees")));
                    Double longitude = parseDouble(text(firstElement(firstElement(trackpoint, "Position"), "LongitudeDegrees")));
                    Double altitude = parseDouble(text(firstElement(trackpoint, "AltitudeMeters")));
                    Double distanceMeters = parseDouble(text(firstElement(trackpoint, "DistanceMeters")));
                    Integer heartRateBpm = parseInteger(text(firstElement(firstElement(trackpoint, "HeartRateBpm"), "Value")));

                    put(point, "recordedAt", recordedAt);
                    put(point, "latitude", latitude);
                    put(point, "longitude", longitude);
                    put(point, "altitudeMeters", altitude);
                    put(point, "distanceMeters", distanceMeters);
                    put(point, "heartRateBpm", heartRateBpm);
                    if (!point.isEmpty()) {
                        trackpoints.add(point);
                    }

                    if (heartRateBpm != null && (maxHeartRate == null || heartRateBpm > maxHeartRate)) {
                        maxHeartRate = heartRateBpm;
                    }
                    if (distanceMeters != null) {
                        totalDistance = distanceMeters;
                    }
                    if (altitude != null) {
                        minAltitude = minAltitude == null ? altitude : Math.min(minAltitude, altitude);
                        maxAltitude = maxAltitude == null ? altitude : Math.max(maxAltitude, altitude);
                    }
                    if (latitude != null && longitude != null) {
                        if (startLatitude == null) {
                            startLatitude = latitude;
                            startLongitude = longitude;
                        }
                        endLatitude = latitude;
                        endLongitude = longitude;
                    }
                }
            }

            if (totalDistance == null) {
                totalDistance = laps.stream()
                        .map(lap -> lap.get("distanceMeters"))
                        .filter(Double.class::isInstance)
                        .map(Double.class::cast)
                        .reduce(Double::sum)
                        .orElse(null);
            }

            return new FitbitTcxData(
                    sport,
                    laps,
                    trackpoints,
                    totalDistance,
                    maxHeartRate,
                    startLatitude,
                    startLongitude,
                    endLatitude,
                    endLongitude,
                    maxAltitude,
                    minAltitude
            );
        } catch (Exception ignored) {
            return FitbitTcxData.empty();
        }
    }

    private static Element firstElement(Element parent, String localName) {
        if (parent == null) {
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element element && localName.equals(element.getLocalName())) {
                return element;
            }
        }
        return null;
    }

    private static List<Element> childElements(Element parent, String localName) {
        List<Element> children = new ArrayList<>();
        if (parent == null) {
            return children;
        }
        NodeList nodes = parent.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node instanceof Element element && localName.equals(element.getLocalName())) {
                children.add(element);
            }
        }
        return children;
    }

    private static String text(Element element) {
        return element != null ? element.getTextContent() : null;
    }

    private static Double parseDouble(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Instant parseInstant(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value).toInstant();
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    record FitbitTcxData(
            String sport,
            List<Map<String, Object>> laps,
            List<Map<String, Object>> trackpoints,
            Double totalDistanceMeters,
            Integer maxHeartRateBpm,
            Double startLatitude,
            Double startLongitude,
            Double endLatitude,
            Double endLongitude,
            Double maxAltitudeMeters,
            Double minAltitudeMeters
    ) {
        static FitbitTcxData empty() {
            return new FitbitTcxData(
                    null,
                    List.of(),
                    List.of(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }
}
