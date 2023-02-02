package de.narvaschach.typesetter;

import org.apache.commons.cli.ParseException;

import java.io.IOException;

public final class Main {
    public static void main(String[] args) throws ParseException, IOException {
        CommandLineLauncher.withArgs(args).launch();
    }

}
