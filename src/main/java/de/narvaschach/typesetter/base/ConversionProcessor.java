package de.narvaschach.typesetter.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ConversionProcessor {
    void process(final InputStream input, final OutputStream output) throws IOException;
}
