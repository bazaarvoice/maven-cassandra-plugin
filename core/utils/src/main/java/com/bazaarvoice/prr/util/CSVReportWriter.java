package com.bazaarvoice.prr.util;

import ognl.Ognl;
import ognl.OgnlException;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * Generic CSV writer that outputs a set values for each specified row.  The column values are specified by
 * OGNL expressions relative to the row object.
 */
public class CSVReportWriter {

	/**
	 * Writes row objects in CSV format to the specified writer.
	 * @param rows
	 * @param includeHeader true if the column headers should be written
	 * @param context the context information needed by the OGNL expressions
	 * @param columns map of OGNL expressions keyed by column title
	 * @param writer
	 */
	public static void writeCSV(List rows, boolean includeHeader, Map context, Map<String, String> columns, Writer writer) throws IOException {
		writeCSV(rows, includeHeader, context, columns, writer, ',');
	}

	/**
	 * Writes row objects in CSV format to the specified writer.
	 * @param rows
	 * @param includeHeader true if the column headers should be written
	 * @param context the context information needed by the OGNL expressions
	 * @param columns map of OGNL expressions keyed by column title
	 * @param writer
	 * @param separatorChar
	 */
	public static void writeCSV(List rows, boolean includeHeader, Map context, Map<String, String> columns, Writer writer, char separatorChar) throws IOException {
		ExcelCSVWriter csvWriter = new ExcelCSVWriter(writer, separatorChar);

		// Pull column titles and OGNL expressions from the map
		String[] columnTitles = columns.keySet().toArray(new String[columns.size()]);
		String[] columnExpressions = columns.values().toArray(new String[columns.size()]);

		// Write out the title row
		if(includeHeader) {
			csvWriter.writeNext(columnTitles);
		}

		// Write out the report rows
		for (Object row : rows) {
			writeRowCSV(row, context, columnExpressions, csvWriter);
		}
	}

	/**
	 * Writes row objects in CSV format to the specified writer.
	 * @param data List of String[]
	 * @param columns map of OGNL expressions keyed by column title
	 * @param writer
	 */
	public static void writeCSV(List<String[]> data, List<String> columns, Writer writer) throws IOException {
		ExcelCSVWriter csvWriter = new ExcelCSVWriter(writer);

		// Pull column titles and OGNL expressions from the map
		String[] columnTitles = columns.toArray(new String[columns.size()]);

		// Write out the title row
		csvWriter.writeNext(columnTitles);

		// Write out the report rows
		for (String[] row : data) {
			csvWriter.writeNext(row);
		}
	}

	/**
	 * Writes one row in CSV format to the specified writer.
	 * @param rowObj
 	 * @param context the context information needed by the OGNL expressions
	 * @param columnExpressions OGNL expression for each column
	 * @param csvWriter
	 */
	private static void writeRowCSV(Object rowObj, Map context, String[] columnExpressions, ExcelCSVWriter csvWriter) throws IOException {
		String[] values = new String[columnExpressions.length];
		for (int i = 0; i < columnExpressions.length; i++) {
			values[i] = getReviewColumnValue(rowObj, context, columnExpressions[i]);
		}

		csvWriter.writeNext(values);
	}

	/**
	 * Attempts to obtain the expression value for the specified row object and OGNL expression.  Returns the
	 * empty string if unable to fetch the OGNL expression.
	 * @param rowObj
	 * @param context the context information needed by the OGNL expressions
	 * @param expression
	 * @return expression value
	 */
	private static String getReviewColumnValue(Object rowObj, Map context, String expression) {
		try {
			Object obj = Ognl.getValue(expression, context, rowObj);
			if (obj != null) {
				return obj.toString();
			}
		} catch(OgnlException oe) {
			return "";
		}

		return "";
	}
}
