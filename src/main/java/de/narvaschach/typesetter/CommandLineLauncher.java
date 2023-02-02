package de.narvaschach.typesetter;

import de.narvaschach.typesetter.base.ConversionProcessor;
import de.narvaschach.typesetter.xskak.XSkakFragmentProcessor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CommandLineLauncher {
    private CommandLineLauncher() {}

    private ConversionProcessor processor;
    private Path input;
    private Path output;

    private boolean append = false;

    private CommandLineLauncher parseArgs(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("format",true, "Selects the output format - currently, only xskakfragment is supported");
        options.addOption("overwrite",false, "Overwrite an existing output file");
        options.addOption("append",false,"Append to an existing file");
        CommandLine cmd = parser.parse(options,args);

        String format = cmd.getOptionValue("format");
        if (format == null)
            processor = new XSkakFragmentProcessor();
        else switch (format.toLowerCase()) {
            case "xskakfragment":
                processor = new XSkakFragmentProcessor();
                break;
            default:
                throw new ParseException("Output format '" + format + "' is not supported");
        }

        if (cmd.getArgList().size() != 2)
            throw new ParseException("Expecting input and output files");
        input = Path.of(cmd.getArgs()[0]);
        output = Path.of(cmd.getArgs()[1]);
        if (!Files.exists(input) || !Files.isReadable(input))
            throw new ParseException("Input file '" + input + "' doesn't exist or cannot be read");

        if (cmd.hasOption("append"))
            append = true;
        else if (!cmd.hasOption("overwrite") && Files.exists(output))
            throw new ParseException("Output file '" + output + "' already exists and neither append nor overwrite mode is enabled");

        return this;
    }

    public static CommandLineLauncher withArgs(String[] args) throws ParseException {
        return new CommandLineLauncher().parseArgs(args);
    }

    public void launch() throws IOException {
        try (InputStream inputStream = Files.newInputStream(input);
             OutputStream outputStream = Files.newOutputStream(output,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                append? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING)
        ) {
            processor.process(inputStream, outputStream);
        }
    }
}
