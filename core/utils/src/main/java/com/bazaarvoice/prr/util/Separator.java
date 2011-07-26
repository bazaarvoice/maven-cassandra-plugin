package com.bazaarvoice.prr.util;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;
import java.util.List;

/**
 * String utility method
 */
public class Separator {

	private final String _next;
	private String _current;

	public Separator(String first, String next) {
		_current = first;
		_next = next;
	}

	public void next(StringBuilder buf) {
		buf.append(_current);
		_current = _next;
	}
}
