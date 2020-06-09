package cz.incad.kramerius.indexer.dates;

import java.util.Date;

/**
 * This class is used only by BiblioModsDateParser and ExtendedFields class as auxiliary structure.
 *
 * @author Aleksei Ermak
 * @see    BiblioModsDateParser
 * @see    cz.incad.kramerius.indexer.ExtendedFields
 */
public class DateQuintet {

    private Date date = null;
    private String dateStr = null;
    private String yearBegin = null;
    private String yearEnd = null;
    private String year = null;

    public Date getDate() {
        return date;
    }

    public String getDateStr() {
        return dateStr;
    }

    public String getYear() {
        return year;
    }

    public String getYearBegin() {
        return yearBegin;
    }

    public String getYearEnd() {
        return yearEnd;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }

    public void setYearBegin(String yearBegin) {
        this.yearBegin = yearBegin;
    }

    public void setYearEnd(String yearEnd) {
        this.yearEnd = yearEnd;
    }

    public void setYear(String year) {
        this.year = year;
    }
}
