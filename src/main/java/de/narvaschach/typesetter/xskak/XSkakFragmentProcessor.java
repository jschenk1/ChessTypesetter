package de.narvaschach.typesetter.xskak;

import de.narvaschach.typesetter.base.ConversionProcessorBase;
import nl.bigo.pp.PGNLexer;
import nl.bigo.pp.PGNParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class XSkakFragmentProcessor extends ConversionProcessorBase {
    @Override
    public void process(final InputStream input, final OutputStream output) throws IOException {
        ParseTree tree = new PGNParser(new CommonTokenStream(new PGNLexer(CharStreams.fromStream(input)))).parse();

        XSkakConverter converter = new XSkakConverter();
        new ParseTreeWalker().walk(converter, tree);
        output.write(converter.getBytes());
    }

}
