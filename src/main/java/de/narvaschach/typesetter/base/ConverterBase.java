package de.narvaschach.typesetter.base;

import nl.bigo.pp.PGNBaseListener;
import nl.bigo.pp.PGNParser;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ConverterBase extends PGNBaseListener {

    // IMPL: games and variations can start with comment - handle comment_before

    protected void onTagSection (final Map<String,String> tags) {}

    protected void onMove (final String moveText, final List<Integer> NAGs) {}

    protected void onMoveSequence (final List<String> moves) {}

    protected void onDiagram() {}

    protected void onComment (final String commentText) {}

    protected void onVariationStart() {}

    protected void onVariationEnd() {}

    private static boolean isDiagramNAG(Integer nag) {
        return nag == 220 //ChessPad
                || nag == 221 //ChessPad
                || nag == 201; //Scid
    }


    private final List<String> moveSequence = new ArrayList<>();

    private void endMoveSequence() {
        if (!moveSequence.isEmpty()) {
            onMoveSequence(moveSequence);
            moveSequence.clear();
        }
    }

    @Override
    public void enterTag_section(final PGNParser.Tag_sectionContext ctx) {
        final Map<String,String> tags = new HashMap<>();
        ctx.tag_pair().stream().forEach(pair -> tags.put(pair.tag_name().getText(), pair.tag_value().getText().substring(1, pair.tag_value().getText().length()-1)));
        this.onTagSection(tags);
    }

    @Override
    public void enterMove(PGNParser.MoveContext ctx) {
        final AtomicBoolean printDiagram = new AtomicBoolean(false);
        final StringBuilder moveTextBuilder = new StringBuilder();
        if (ctx.move_number_indication() != null)
            moveTextBuilder.append(" ").append(ctx.move_number_indication().getText());
        moveTextBuilder.append(" ").append(ctx.san_move().getText());
        if (ctx.SUFFIX_ANNOTATION() != null)
            moveTextBuilder.append(ctx.SUFFIX_ANNOTATION().getText());
        final String moveText = moveTextBuilder.toString();
        final List<Integer> NAGs = new ArrayList<>();
        ctx.NUMERIC_ANNOTATION_GLYPH().stream()
                .map(TerminalNode::getText)
                .map(str -> str.substring(1))
                .map(Integer::parseInt)
                .forEach(nag -> {
                    if (isDiagramNAG(nag))
                        printDiagram.set(true);
                    else
                        NAGs.add(nag);
                });

        onMove(moveText, NAGs);

        final StringBuilder fullMoveText = new StringBuilder();
        fullMoveText.append(moveText);
        if (!NAGs.isEmpty())
            NAGs.stream().forEachOrdered(nag -> fullMoveText.append(" $").append(nag));
        moveSequence.add(fullMoveText.toString());

        if (printDiagram.get()) {
            endMoveSequence();
            onDiagram(); // will need diagram settings from comment in the future
        }
        if (ctx.comment() != null) {
            final String comment = ctx.comment().BRACE_COMMENT().getText();
            if (StringUtils.isNotEmpty(comment)) {
                endMoveSequence();
                onComment(comment.substring(1,comment.length()-1));
            }
        }
    }

    @Override
    public void enterRecursive_variation(PGNParser.Recursive_variationContext ctx) {
        endMoveSequence();
        onVariationStart();
    }
    @Override
    public void exitRecursive_variation(PGNParser.Recursive_variationContext ctx) {
        endMoveSequence();
        onVariationEnd();
    }
}
