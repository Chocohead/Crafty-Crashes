package com.chocohead.cc.smap;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

class SMAPReader implements Closeable {
	private final BufferedReader in;
	private boolean hasPeeked;
	private String linePeek;

	public SMAPReader(Reader in) {
		this.in = new BufferedReader(in);
	}

	public String peekLine() throws IOException {
		if (!hasPeeked) {
			linePeek = in.readLine();
			hasPeeked = true;
		}

		return linePeek;
	}

	public String nextLine() throws IOException {
		if (hasPeeked) {
			hasPeeked = false;
			return linePeek;
		}

		return in.readLine();
	}

	private static String trimStart(String value) {
		//return CharMatcher.whitespace().trimLeadingFrom(value);
		return trimFrom(value, 0);
	}

	private static String trimFrom(String value, int start) {
		for (int end = value.length(); start < end; start++) {
			if (!Character.isWhitespace(value.charAt(start))) {
				break;
			}
		}

		return start > 0 ? value.substring(start) : value;
	}

	public char chompHeader() throws IOException {
		String line = peekLine();
		if (line == null) throw new EOFException("Expected header but found EOF");

		char out;
		if (line.length() < 2 || line.charAt(0) != '*' || (out = line.charAt(1)) == ' ' || out == '\t') {
			throw new InvalidFormat("Expected header but found: " + line);
		}

		linePeek = trimFrom(linePeek, 2);
		hasPeeked = !linePeek.isEmpty();
		return out;
	}

	public String nextString() throws IOException {
		return makeString(nextLine());			
	}

	public String chompString() throws IOException {
		try {
			return makeString(peekLine());
		} finally {
			hasPeeked = false; //All gone
		}
	}

	private String makeString(String line) throws IOException {
		if (line == null) throw new EOFException("Expected non-asterisk string but found EOF");

		line = trimStart(line);
		if (line.isEmpty() || line.charAt(0) == '*') {
			throw new InvalidFormat("Expected non-asterisk string but found: " + line);
		}

		return line;
	}

	public int nextNumber() throws IOException {
		String line = nextLine();
		if (line == null) throw new EOFException("Expected number but found EOF");

		line = line.trim();
		if (!line.isEmpty() && line.charAt(0) != '+') {
			try {
				return Integer.parseUnsignedInt(line);
			} catch (NumberFormatException e) {
				//Fall through
			}
		}

		throw new InvalidFormat("Expected number but found: " + line);
	}

	public int chompNumber() throws IOException {
		String line = peekLine();
		if (line == null) throw new EOFException("Expected number but found EOF");

		int start = 0;
		for (int end = line.length(); start < end; start++) {
			if (!Character.isWhitespace(line.charAt(start))) {
				break;
			}
		}

		int to = start;
		for (int end = line.length(); to < end; to++) {
			if (!Character.isDigit(line.charAt(to))) {
				break;
			}
		}

		if (to > start) {
			try {
				int out = Integer.parseUnsignedInt(line.substring(start, to));

				linePeek = trimFrom(linePeek, to);
				hasPeeked = !linePeek.isEmpty();

				return out;
			} catch (NumberFormatException e) {
				//Fall through
			}
		}

		throw new InvalidFormat("Expected number but found: " + line);
	}

	public String chompTo(char expectedChar) throws IOException {
		String line = peekLine();
		if (line == null) throw new EOFException("Expected line but found EOF");

		for (int limit = 0, end = line.length(); limit < end; limit++) {
			if (line.charAt(limit) == expectedChar) {
				if (++limit == end) {
					hasPeeked = false; //Nothing left
				} else {
					linePeek = trimFrom(line, limit);
					hasPeeked = !linePeek.isEmpty();
				}

				return line.substring(0, limit);
			}
		}

		throw new InvalidFormat("Expected " + expectedChar + " but found: " + line);
	}

	public boolean expectIfMore(char expectedChar) throws IOException {
		return hasPeeked && expect(expectedChar);
	}

	public boolean expect(char expectedChar) throws IOException {
		String line = peekLine();
		if (line == null) throw new EOFException("Expected line but found EOF");

		if (!line.isEmpty() && line.charAt(0) == expectedChar) {
			linePeek = line.substring(1);
			hasPeeked = !linePeek.isEmpty();
			return true;
		} else {
			return false;
		}
	}

	public void skip(int chars) throws IOException {
		String line = peekLine();
		if (line == null) throw new EOFException("Expected line but found EOF");

		if (line.length() < chars) {
			throw new InvalidFormat("Expected at least " + chars + " characters but found: " + line);
		}

		linePeek = linePeek.substring(chars);
		hasPeeked = !linePeek.isEmpty();
	}

	public void ensureLineComplete() throws IOException {
		if (hasPeeked) {//If there is still peeked line left it wasn't fully read
			throw new InvalidFormat("Expected CR but found: " + linePeek);
		}
	}

	public void ensureComplete() throws IOException {
		String line = in.readLine();

		if (line != null) {
			throw new InvalidFormat("Expected EOF but found: " + line);
		}
	}

	@Override
	public void close() throws IOException {
		in.close();
	}
}