package com.chocohead.cc.smap;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SMAP {
	public final String generatedFileName;
	private final String defaultStratum;
	private final Map<String, Stratum> stratums = new HashMap<>();
	private final Map<String, List<String>> vendorSections = new HashMap<>();

	public static SMAP forResolved(Reader in) throws IOException {
		return new SMAP(in);
	}

	public static SMAP forResolved(String in) {
		try {
			return new SMAP(new StringReader(in));
		} catch (InvalidFormat | EOFException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		} catch (IOException e) {
			throw new AssertionError("Impossible?", e);
		}
	}

	private SMAP(Reader in) throws IOException {
		try (SMAPReader reader = new SMAPReader(in)) {
			String first = reader.nextLine();
			if (!"SMAP".equals(first)) {
				throw new InvalidFormat("First line expected SMAP but had " + first);
			}

			generatedFileName = reader.nextString(); //Should match source attribute
			defaultStratum = reader.nextString(); //Must be specified (i.e. not blank)
			if (defaultStratum.isEmpty()) throw new InvalidFormat("Expected resolved SMAP but default stratum is unspecified");

			StratumBuilder stratum = null;
			boolean seenFile = false;
			boolean seenLines = false;
			out: while (true) {
				switch (reader.chompHeader()) {
				case 'S': {//Stratum
					String id = reader.chompString();
					if ("Java".equals(id)) throw new UnsupportedOperationException("Resolved SMAPs should not have the final-source stratum");

					if (stratum != null) {
						stratums.put(stratum.id, stratum.validate(seenFile, seenLines));
						seenFile = seenLines = false;
					}

					stratum = new StratumBuilder(id);
					reader.ensureLineComplete();
					break;
				}

				case 'F': //File
					if (stratum == null) throw new InvalidFormat("File section declared before stratum");
					if (seenFile) throw new InvalidFormat("Duplicate file sections declared for " + stratum.id + " stratum");
					seenFile = true;

					reader.ensureLineComplete();
					for (String line = reader.peekLine(); line.isEmpty() || line.charAt(0) != '*'; line = reader.peekLine()) {
						boolean hasPath = reader.expect('+');

						int id = reader.chompNumber();
						String name = reader.chompString();
						reader.ensureLineComplete();
						String path = hasPath ? reader.nextLine() : null;

						stratum.addFile(id, name, path);
						reader.ensureLineComplete();
					}
					break;

				case 'L': //Line
					if (stratum == null) throw new InvalidFormat("Line section declared before stratum");
					if (seenLines) throw new InvalidFormat("Duplicate line sections declared for " + stratum.id + " stratum");
					seenLines = true;

					reader.ensureLineComplete();
					for (String line = reader.peekLine(); line.isEmpty() || line.charAt(0) != '*'; line = reader.peekLine()) {
						int input = reader.chompNumber();

						int file;
						if (reader.expect('#')) {
							file = reader.chompNumber();
						} else {
							file = -1;
						}

						int repeat;
						if (reader.expect(',')) {
							repeat = reader.chompNumber();
						} else {
							repeat = 1;
						}

						if (!reader.expect(':')) {
							throw new InvalidFormat("Expected : but found " + line);
						}

						int output = reader.chompNumber();

						int increment;
						if (reader.expectIfMore(',')) {
							increment = reader.chompNumber();
						} else {
							increment = 1;
						}

						stratum.addLines(input, file, repeat, output, increment);
						reader.ensureLineComplete();
					}
					break;

				case 'O': //Open embedded
				case 'C': //Close embedded
					throw new UnsupportedOperationException("Embedded SMAP found, expected resolved SMAP");

				case 'V': {//Vendor
					reader.ensureLineComplete();
					String id = reader.nextString();
					List<String> vendorInfo = new ArrayList<>();

					for (String info = reader.peekLine(); info.isEmpty() || info.charAt(0) != '*'; info = reader.peekLine()) {
						vendorInfo.add(reader.nextLine());
					}

					vendorSections.put(id, vendorInfo.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(vendorInfo));
					break;
				}

				case 'E': //End
					if (stratum != null) {
						stratums.put(stratum.id, stratum.validate(seenFile, seenLines));
					} else {
						throw new InvalidFormat("No stratum found before end section");
					}

					reader.ensureLineComplete();
					reader.ensureComplete();
					break out;

				default: //Future
					reader.ensureLineComplete();

					for (String futureInfo = reader.peekLine(); futureInfo.isEmpty() || futureInfo.charAt(0) != '*'; futureInfo = reader.peekLine()) {
						reader.nextLine(); //Flush the last peeked line out
					}

					break; //Any unknown sections must be ignored without error
				}
			}
		}
	}

	public Stratum getDefaultStratum() {
		return getStratum(defaultStratum);
	}

	public Stratum getStratum(String id) {
		return stratums.get(id);
	}

	public Collection<Stratum> getStratums() {
		return stratums.values();
	}

	public Set<String> getVendors() {
		return vendorSections.keySet();
	}

	public List<String> getVendorInfo(String id) {
		return vendorSections.get(id);
	}
}