package de.narvaschach.typesetter.xskak;

import de.narvaschach.typesetter.base.ConverterBase;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

public class XSkakConverter extends ConverterBase{


    private final List<String> lines = new ArrayList<>();
    private StringBuilder sb;

    private final Stack<String> varIDs = new Stack<>();

    private final List<String> nextMoveSeqOptions = new ArrayList<>();

    public XSkakConverter() {
        sb = new StringBuilder();
    }

    public byte[] getBytes() {
        linebreak();
        return (StringUtils.join(lines,"\\\\\n")).getBytes(StandardCharsets.UTF_8);
    }

    private void linebreak() {
        linebreak(false);
    }

    private void linebreak(boolean force) {
        if (!force && sb.isEmpty()) return; // avoid double line breaks

        lines.add(sb.toString());
        sb = new StringBuilder();
    }

    protected void onTagSection (final Map<String,String> tags) {
        final String curGameID = UUID.randomUUID().toString();

        varIDs.clear();
        varIDs.push(curGameID);

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

            linebreak();
            sb.append("$\\bullet$ ").append(tags.get("Black"));
            if (tags.containsKey("BlackDWZ"))
                sb.append(" (").append(tags.get("BlackDWZ")).append(")");
            else if (tags.containsKey("BlackElo"))
                sb.append(" (").append(tags.get("BlackElo")).append(")");

            final String siteOrEvent = StringUtils.firstNonBlank(tags.get("Site"), tags.get("Event"));
            final String year = StringUtils.isNotEmpty(tags.get("Date")) && tags.get("Date").length()>=4? tags.get("Date").substring(0,4) : "";
            if (siteOrEvent != null) {
                linebreak();
                sb.append(siteOrEvent);
                if (!siteOrEvent.contains(year))
                    sb.append(" " + year);
            }
            else if (StringUtils.isNotEmpty(year) && !"????".equals(year)) {
                linebreak();
                sb.append(year);
            }
            if (StringUtils.isNotEmpty(tags.get("ECO"))) {
                linebreak();
                sb.append(tags.get("ECO"));
            }
            linebreak();
            sb.append("}\n");

            if (tags.containsKey("FEN")) {
                sb.append("\\chessboard ");
                linebreak();
            }
        }
    }

    protected void onMoveSequence (final List<String> moves) {
        if (varIDs.size() <= 1)
            linebreak(true); // extra line break when returning to main line
        sb.append("\\mainline");
        if (!nextMoveSeqOptions.isEmpty()) {
            sb.append("[").append(StringUtils.join(nextMoveSeqOptions, ",")).append("]");
            nextMoveSeqOptions.clear();
        }
        sb.append("{");
        moves.stream().forEach(sb::append);
        sb.append("}");

    }

    protected void onVariationStart() {
        linebreak();
        nextMoveSeqOptions.add("invar");
        final String newVarID = UUID.randomUUID().toString();
        final String parentID = varIDs.peek();
        varIDs.push(newVarID);
        sb.append("\\newchessgame[newvar=").append(parentID).append(",id=").append(newVarID).append("]\n");
    }

    protected void onVariationEnd() {
        linebreak();
        varIDs.pop();
        sb.append("\\resumechessgame[id=").append(varIDs.peek()).append("]\n");
        nextMoveSeqOptions.add("outvar");
    }

    protected void onGameTermination(final String result) {
        // IMPL: possible missing outvar
        linebreak();
        sb.append("\\textbf{").append(result).append("}");
    }

    protected void onDiagram() {
        linebreak();
        sb.append("\\chessboard[");
        if (varIDs.size() <= 1)
            sb.append("smallboard,fontsize=17pt");
        else if (varIDs.size() == 2)
            sb.append("tinyboard,fontsize=13pt");
        else
            sb.append("tinyboard,fontsize=13pt,piececolor=black!70,fieldcolor=black!70,"+
                    "bordercolor=black!70,hlabel=false,vlabel=false,setfontcolors");
        sb.append("]");
        linebreak();
    }

    protected void onComment(final String commentText) {
        if (Character.isUpperCase(commentText.trim().charAt(0)))
            linebreak();
        else
            sb.append("\n");
        // IMPL: may produce illegal TeX when the comment contains certain symbols such as {.
        sb.append(commentText);
        linebreak();
    }
}
