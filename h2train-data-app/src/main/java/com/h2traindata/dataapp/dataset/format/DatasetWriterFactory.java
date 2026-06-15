package com.h2traindata.dataapp.dataset.format;

import com.h2traindata.dataapp.dataset.exception.UnsupportedDatasetFormatException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DatasetWriterFactory {

    private final Map<DatasetFormat, DatasetWriter> writers;

    public DatasetWriterFactory(List<DatasetWriter> writers) {
        Map<DatasetFormat, DatasetWriter> byFormat = new EnumMap<>(DatasetFormat.class);
        writers.forEach(writer -> byFormat.put(writer.format(), writer));
        this.writers = Map.copyOf(byFormat);
    }

    public DatasetWriter writerFor(DatasetFormat format) {
        DatasetWriter writer = writers.get(format);
        if (writer == null) {
            throw new UnsupportedDatasetFormatException(format == null ? null : format.value());
        }
        return writer;
    }
}
