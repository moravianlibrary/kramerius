package cz.incad.kramerius.indexer.dates;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import cz.incad.kramerius.FedoraNamespaceContext;

import cz.incad.kramerius.security.impl.criteria.mw.DateLexer;
import cz.incad.kramerius.security.impl.criteria.mw.DatesParser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import javax.xml.xpath.*;

import java.io.StringReader;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * This class is used to parse the years of document publication from BIBLIO MODS data stream.
 * It uses precompiled regular and XPath expressions to retrieve date nodes and parse them later.
 * Parsed years are returned in a special auxiliary structure. Before that, the class caches them
 * by document uuid.
 *
 * @author Aleksei Ermak
 * @see    DateQuintet
 */
public class BiblioModsDateParser {

    /* Structure to cache years for certain uuid */
    private final HashMap<String, DateQuintet> dateCache;
    private static final Logger logger = Logger.getLogger(BiblioModsDateParser.class.getName());

    /* XPath expressions for BIBLIO MODS date nodes extraction */
    private final String prefix = "//mods:mods/";
    private List<XPathExpression> modsDateXPathExps;
    private final List<String> modsDateXPathStrs = Arrays.asList(
            prefix + "mods:part/mods:date/text()",
            prefix + "mods:originInfo[@transliteration='publisher']/mods:dateIssued/text()",
            prefix + "mods:originInfo/mods:dateIssued/text()"
    );

    /* Regular expressions for parsing years from text */
    private List<Pattern> yearRegexPatterns;
    private final List<String> yearRegexStrs = Arrays.asList(
            "(?<![0-9])[0-9]{3}(?![0-9])",   // 800, 999
            "(?<![0-9])[0-9]{4}(?![0-9])",   // 1941, 1945
            "(?<![0-9])[0-9]{3}-(?![0-9])",  // 194-, 199-
            "(?<![0-9])[0-9]{2}--(?![0-9])", // 18--, 19--
            "(?<![0-9])[\\^]{4}(?![0-9])"    // ^^^^
    );

    public BiblioModsDateParser() {
        dateCache = new HashMap<>();
        compileModsDateXPaths();
        compileYearRegexPatterns();
    }

    /**
     * Compiles XPath expressions to retrieve date nodes from BIBLIO MODS data stream later.
     */
    private void compileModsDateXPaths() {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new FedoraNamespaceContext());
        modsDateXPathExps = new ArrayList<>();
        for (String dateXPathStr : modsDateXPathStrs) {
            try {
                modsDateXPathExps.add(xpath.compile(dateXPathStr));
            } catch (XPathExpressionException e) {
                logger.warning(
                        "Can't compile XPath expressions \"" +
                                dateXPathStr + "\" to retrieve BIBLIO MODS date nodes!"
                );
                logger.warning(e.getMessage());
            }
        }
    }

    /**
     * Compiles regular expressions to parse years from BIBLIO MODS date nodes content later.
     */
    private void compileYearRegexPatterns() {
        yearRegexPatterns = new ArrayList<>();
        for (String yearRegexStr : yearRegexStrs) {
            yearRegexPatterns.add(Pattern.compile(yearRegexStr));
        }
    }

    /**
     * Returns years for uuid if they were cached, otherwise returns null.
     *
     * @param  uuid uuid to check
     * @return      dates for uuid or null
     * @see    DateQuintet
     */
    public DateQuintet checkInCache(String uuid) {
        return dateCache.getOrDefault(uuid, null);
    }

    /**
     * Retrieves date nodes from BIBLIO MODS data stream by precompiled XPath expressions,
     * then parses the textual contents of the nodes by precompiled regular expressions.
     * Fills a quintet structure by parsed years and stores it to the date cache using document uuid.
     * Returns filled quintet structure or null if BIBLIO MODS has no date nodes.
     *
     * @param  biblioMods BIBLIO MODS data stream
     * @param  uuid       uuid to save parsed dates to cache
     * @return            years in an auxiliary structure or null
     * @throws XPathExpressionException
     * @see    DateQuintet
     */
    public DateQuintet extractYearsFromBiblioMods(Document biblioMods, String uuid)
            throws XPathExpressionException {

        List<Node> dateNodes = getDateNodes(biblioMods);
        if (dateNodes.isEmpty()) {
            return null;  // BIBLIO MODS has no dates
        }

        // parse all date nodes in MODS, save dates to structure
        DateQuintet dateQuintet = new DateQuintet();
        for (Node dateNode : dateNodes) {
            parseNodeAndFillDateQuintet(dateNode, dateQuintet);
        }
        setBeginAndEndYearsIfEmpty(dateQuintet, dateQuintet.getYear());

        // save prepared quintet to the date cache
        dateCache.put(uuid, dateQuintet);

        return dateQuintet;
    }

    /**
     * Retrieves nodes from org.w3c.dom.Document by different precompiled XPath expressions
     * and returns list of that nodes.
     *
     * @param  doc XML document to retrieve nodes from it
     * @return     list of retrieved nodes
     * @throws XPathExpressionException
     */
    private List<Node> getDateNodes(Document doc) throws XPathExpressionException {
        List<Node> resultNodeList = new ArrayList<>();
        for (XPathExpression dateExp : modsDateXPathExps) {
            NodeList nodes = (NodeList) dateExp.evaluate(doc, XPathConstants.NODESET);
            if (nodes.getLength() > 0) {
                for (int i = 0; i < nodes.getLength(); i++) {
                    resultNodeList.add(nodes.item(i));
                }
            }
        }
        return resultNodeList;
    }

    /**
     * Parses a document node with the text content and fills a helper structure.
     *
     * @param dateNode     document node containing the date in string representation
     * @param dateQuintet  auxiliary structure to fill
     */
    private void parseNodeAndFillDateQuintet(Node dateNode, DateQuintet dateQuintet) {
        NamedNodeMap attributes = dateNode.getParentNode().getAttributes();
        String dateStr = dateNode.getTextContent();

        if (attributes == null || attributes.getLength() == 0 ||
                attributes.getNamedItem("point") == null) {
            parseYearsFromDateStr(dateStr, dateQuintet);
            dateQuintet.setDateStr(dateStr);
        }
        else {
            Node point = attributes.getNamedItem("point");
            if (point == null) return;

            if ("start".equals(point.getNodeValue())) {
                dateQuintet.setYearBegin(dateStr);
            } else {
                dateQuintet.setYearEnd(dateStr);
            }
        }
    }

    /**
     * Gets all possible years of publication and fills an auxiliary quintet structure.
     * If there is more than one year, chooses minimal and maximal and set them as beginning and end years
     * of publication. In this case general year is the end year of publication.
     *
     * @param dateStr     string to parse
     * @param dateQuintet auxiliary structure to fill
     */
    private void parseYearsFromDateStr(String dateStr, DateQuintet dateQuintet) {
        // apply date patterns and get all possible years from dateStr
        List<String> yearsStr = getAllMatchedYears(dateStr);
        if (yearsStr.size() > 1) {
            // several years have been found -> setup begin and end dates
            List<Integer> yearsInt = yearsStr.stream().map(Integer::valueOf).collect(Collectors.toList());
            dateQuintet.setYearBegin(String.valueOf(Collections.min(yearsInt)));
            String yearEnd = String.valueOf(Collections.max(yearsInt));
            dateQuintet.setYearEnd(yearEnd);
            dateQuintet.setYear(yearEnd);
        } else if (!yearsStr.isEmpty()) {
            dateQuintet.setYear(yearsStr.get(0));
        }
        String year = dateQuintet.getYear();
        dateQuintet.setDate(parseDateOrSetDefault(dateStr, year));
    }

    /**
     * If the years of the beginning or end of publication are empty,
     * fills them with the general year of publication.
     *
     * @param dateQuintet auxiliary structure to check and fill
     * @param year        default year
     */
    private void setBeginAndEndYearsIfEmpty(DateQuintet dateQuintet, String year) {
        if (dateQuintet.getYearBegin() == null) {
            dateQuintet.setYearBegin(year);
        }
        if (dateQuintet.getYearEnd() == null) {
            dateQuintet.setYearEnd(year);
        }
    }

    /**
     * Tries to parse original string containing date of publication.
     * If can't parse returns date specified by general year of publication.
     *
     * @param  dateStr     string containing the date
     * @param  defaultYear year to parse if the date in string can't be parsed
     * @return             date of publication or null
     */
    private Date parseDateOrSetDefault(String dateStr, String defaultYear) {
        Date publicationDate = parseDateFromStr(dateStr);
        if (publicationDate == null)
            publicationDate = parseDateFromStr(defaultYear);
        return publicationDate;
    }

    /**
     * Parses extracted date in textual representation by different precompiled regular expressions.
     * In parsed years replaces characters denoting an uncertain publication date.
     *
     * @param  dateStr  string to parse
     * @return          list of all possible years of publication without any extra character
     */
    private List<String> getAllMatchedYears(String dateStr) {
        List<String> years = new ArrayList<>();
        for (Pattern pattern : yearRegexPatterns) {
            Matcher matcher = pattern.matcher(dateStr);
            while (matcher.find()) {
                for (int i = 0, groupCount = matcher.groupCount(); i <= groupCount; i++) {
                    years.add(replaceNonDigit(matcher.group(i)));
                }
            }
        }
        return years;
    }

    /**
     * Replaces non-digit characters denoting an uncertain publication date from string.
     *
     * @param  dateStr string to replace characters in it
     * @return         string without characters denoting an uncertain publication date
     */
    private String replaceNonDigit(String dateStr) {
        dateStr = dateStr.replaceAll("\\^", "9"); // ^^^^ -> 9999
        dateStr = dateStr.replaceAll("-", "0");   // 19-- -> 1900
        return dateStr;
    }

    /**
     * Parses date from string.
     *
     * @param  dateStr string to parse
     * @return         date or null
     */
    private Date parseDateFromStr(String dateStr) {
        try {
            DatesParser p = new DatesParser(new DateLexer(new StringReader(dateStr)));
            return p.dates();
        } catch (NullPointerException | RecognitionException | TokenStreamException e) {
            return null;
        }
    }
}
