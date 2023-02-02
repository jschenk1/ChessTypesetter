package de.narvaschach.typesetter.xskak;

import de.narvaschach.typesetter.base.ConverterBase;
import nl.bigo.pp.PGNParser;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class XSkakConverter extends ConverterBase{

    private String curGameID = "";
    private final StringBuilder sb;

    private int variationDepth = 0;

    private List<String> nextMoveSeqOptions = new ArrayList<>();

    public XSkakConverter() {
        sb = new StringBuilder();
    }

    public byte[] getBytes() {
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }


    protected void onTagSection (final Map<String,String> tags) {
        curGameID = UUID.randomUUID().toString();
        variationDepth = 0;
        sb.append("\\newchessgame[id=" + curGameID);
        if (tags.containsKey("FEN")) {
            final String[] fenParts = tags.get("FEN").split(" ");
            if (fenParts.length != 6)
            {
                throw new RuntimeException("Invalid FEN " + tags.get("FEN"));
            }
            sb.append(",setfen=" + tags.get("FEN") + ",moveid=" + fenParts[5] + fenParts[1]);
        }
        sb.append("]\n");
        if (tags.containsKey("White") && tags.containsKey("Black")) {
            sb.append("{\\bf\n$\\circ$ ").append(tags.get("White"));
            if (tags.containsKey("WhiteDWZ"))
                sb.append(" (").append(tags.get("WhiteDWZ")).append(")");
            else if (tags.containsKey("WhiteElo"))
                sb.append(" (").append(tags.get("WhiteElo")).append(")");

            sb.append("\\\\\n$\\bullet$ ").append(tags.get("Black"));
            if (tags.containsKey("BlackDWZ"))
                sb.append(" (").append(tags.get("BlackDWZ")).append(")");
            else if (tags.containsKey("BlackElo"))
                sb.append(" (").append(tags.get("BlackElo")).append(")");

            final String siteOrEvent = StringUtils.firstNonBlank(tags.get("Site"), tags.get("Event"));
            final String year = StringUtils.isNotEmpty(tags.get("Date")) && tags.get("Date").length()>=4? tags.get("Date").substring(0,4) : "";
            if (siteOrEvent != null) {
                sb.append("\\\\\n").append(siteOrEvent);
                if (!siteOrEvent.contains(year))
                    sb.append(" " + year);
            }
            else if (StringUtils.isNotEmpty(year))
                sb.append("\\\\\n").append(year);
            if (StringUtils.isNotEmpty(tags.get("ECO")))
                sb.append("\\\\\n").append(tags.get("ECO"));
            sb.append("\\\\\n}\n");

            if (tags.containsKey("FEN"))
                sb.append("\\chessboard \\\\\n");
        }
    }

    protected void onMoveSequence (final List<String> moves) {
        // TODO: this is sufficient for todays training material, but \variation doesn't update xskaks
        //  internal game state. This effectively means that diagrams within variations don't work. They
        //  always show the mainline position.
        if (variationDepth==0)
            sb.append("\\mainline");
        else
            sb.append("\\variation");
        if (!nextMoveSeqOptions.isEmpty()) {
            sb.append("[").append(StringUtils.join(nextMoveSeqOptions, ",")).append("]");
            nextMoveSeqOptions.clear();
        }
        sb.append("{");
        moves.stream().forEach(sb::append);
        sb.append("}");
    }

    protected void onVariationStart() {
        variationDepth++;
        nextMoveSeqOptions.add("invar");
        sb.append(" \\\\\n");
    }

    protected void onVariationEnd() {
        variationDepth--;
        nextMoveSeqOptions.add("outvar");
        sb.append(" \\\\\n");
    }

    protected void onDiagram() {
        sb.append("\\\\\n\\chessboard \\\\\n");
    }

    protected void onComment(final String commentText) {
        if (!Character.isLowerCase(commentText.trim().charAt(0)))
            sb.append("\\\\");
        // IMPL: may produce illegal TeX when the comment contains certain symbols such as {.
        sb.append("\n").append(commentText).append(" \\\\\n");
    }
}
