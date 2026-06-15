package com.h2traindata.dataapp.dataset.format;

public record DatasetRenderedResponse(
        byte[] body,
        String contentType,
        String filename
) {
}
