package uk.ac.qub.dkeenan21.entropy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the EntropyComputer class
 */
public class EntropyComputerTest {
	private Map<String, Integer> changeSetSummary;
	private EntropyComputer entropyComputer;

	@BeforeEach
	public void initialise() {
		entropyComputer = new EntropyComputer();
		changeSetSummary = new TreeMap<>();
	}

	@Test
	public void computeEntropyOfChangeSet_zeroFilesChanged() {
		final double entropy = entropyComputer.computeEntropyOfChangeSet(changeSetSummary);
		assertEquals(0.0, entropy);
	}

	@Test
	public void computeEntropyOfChangeSet_singleFileChanged_zeroLinesChanged() {
		// note: zero-line changes are anomalous should be ignored in computation
		changeSetSummary.put("File A", 0);
		final double entropy = entropyComputer.computeEntropyOfChangeSet(changeSetSummary);
		assertEquals(0.0, entropy);
	}

	@Test
	public void computeEntropyOfChangeSet_singleFileChanged_singleLineChanged() {
		changeSetSummary.put("File A", 1);
		final double entropy = entropyComputer.computeEntropyOfChangeSet(changeSetSummary);
		assertEquals(0.0, entropy);
	}

	@Test
	public void computeEntropyOfChangeSet_singleFileChanged_multipleLinesChanged() {
		changeSetSummary.put("File A", 25);
		final double entropy = entropyComputer.computeEntropyOfChangeSet(changeSetSummary);
		assertEquals(0.0, entropy);
	}

	@Test
	public void computeEntropyOfChangeSet_multipleFilesChanged_zeroLinesChangedInEach() {
		// note: zero-line changes are anomalous should be ignored in computation
		changeSetSummary.put("File A", 0);
		changeSetSummary.put("File B", 0);
		changeSetSummary.put("File C", 0);
		changeSetSummary.put("File D", 0);
		changeSetSummary.put("File E", 0);
		changeSetSummary.put("File F", 0);
		changeSetSummary.put("File G", 0);
		changeSetSummary.put("File H", 0);
		final double entropy = entropyComputer.computeEntropyOfChangeSet(changeSetSummary);
		assertEquals(0.0, entropy);
	}

	@Test
	public void computeEntropyOfChangeSet_multipleFilesChanged_singleLineChangedInEach() {
		changeSetSummary.put("File A", 1);
		changeSetSummary.put("File B", 1);
		changeSetSummary.put("File C", 1);
		changeSetSummary.put("File D", 1);
		changeSetSummary.put("File E", 1);
		changeSetSummary.put("File F", 1);
		changeSetSummary.put("File G", 1);
		changeSetSummary.put("File H", 1);
		final double entropy = entropyComputer.computeEntropyOfChangeSet(changeSetSummary);
		assertEquals(3.0, entropy);
	}

	@Test
	public void computeEntropyOfChangeSet_multipleFilesChanged_multipleLinesChangedInEach_changesEvenlyDistributed() {
		changeSetSummary.put("File A", 25);
		changeSetSummary.put("File B", 25);
		changeSetSummary.put("File C", 25);
		changeSetSummary.put("File D", 25);
		changeSetSummary.put("File E", 25);
		changeSetSummary.put("File F", 25);
		changeSetSummary.put("File G", 25);
		changeSetSummary.put("File H", 25);
		final double entropy = entropyComputer.computeEntropyOfChangeSet(changeSetSummary);
		assertEquals(3.0, entropy);
	}

	@Test
	public void computeEntropyOfChangeSet_multipleFilesChanged_multipleLinesChangedInEach_changesUnevenlyDistributed() {
		changeSetSummary.put("File A", 33);
		changeSetSummary.put("File B", 25);
		changeSetSummary.put("File C", 10);
		changeSetSummary.put("File D", 9);
		changeSetSummary.put("File E", 9);
		changeSetSummary.put("File F", 7);
		changeSetSummary.put("File G", 5);
		changeSetSummary.put("File H", 2);
		final double entropy = entropyComputer.computeEntropyOfChangeSet(changeSetSummary);
		assertEquals(2.582851523906001, entropy);
	}

	@Test
	public void computeEntropyOfChangeSet_multipleFilesChanged_changesUnevenlyDistributed2() {
		changeSetSummary.put("File A", 250);
		changeSetSummary.put("File B", 250);
		changeSetSummary.put("File C", 125);
		changeSetSummary.put("File D", 125);
		changeSetSummary.put("File E", 125);
		changeSetSummary.put("File F", 125);
		final double entropy = entropyComputer.computeEntropyOfChangeSet(changeSetSummary);
		assertEquals(2.5, entropy);
	}

	@Test
	public void computeEntropyOfChangeSet_multipleFilesChanged_changesUnevenlyDistributed3() {
		changeSetSummary.put("File A", 3);
		changeSetSummary.put("File B", 3);
		changeSetSummary.put("File C", 6);
		final double entropy = entropyComputer.computeEntropyOfChangeSet(changeSetSummary);
		assertEquals(1.5, entropy);
	}

	@Test
	public void computeEntropyOfChangeSet_multipleFilesChanged_multipleLinesChangedInEach_changesVeryUnevenlyDistributed() {
		changeSetSummary.put("File A", 71);
		changeSetSummary.put("File B", 10);
		changeSetSummary.put("File C", 5);
		changeSetSummary.put("File D", 4);
		changeSetSummary.put("File E", 3);
		changeSetSummary.put("File F", 3);
		changeSetSummary.put("File G", 2);
		changeSetSummary.put("File H", 2);
		final double entropy = entropyComputer.computeEntropyOfChangeSet(changeSetSummary);
		assertEquals(1.6141487706500264, entropy);
	}

	@Test
	public void computeEntropyOfChangeSet_multipleFilesChanged_mixOfZeroAndSingleAndMultipleLinesChangedInEach() {
		changeSetSummary.put("File A", 18);
		changeSetSummary.put("File B", 12);
		changeSetSummary.put("File C", 7);
		changeSetSummary.put("File D", 1);
		changeSetSummary.put("File E", 1);
		changeSetSummary.put("File F", 1);
		changeSetSummary.put("File G", 0); // note: zero-line changes should be ignored in computation
		changeSetSummary.put("File H", 0);
		final double entropy = entropyComputer.computeEntropyOfChangeSet(changeSetSummary);
		assertEquals(1.878685982661894, entropy);
	}

	@Test
	public void computeEntropyOfFileInChangeSet_fileNotPresentInChangeSet() {
		changeSetSummary.put("File A", 10);
		changeSetSummary.put("File B", 10);
		final double entropy = entropyComputer.computeEntropyOfFileInChangeSet(changeSetSummary, "File C");
		assertEquals(0.0, entropy);
	}

	@Test
	public void computeEntropyOfFileInChangeSet_zeroLinesChangedInFile() {
		changeSetSummary.put("File A", 10);
		changeSetSummary.put("File B", 10);
		changeSetSummary.put("File C", 0);
		final double entropy = entropyComputer.computeEntropyOfFileInChangeSet(changeSetSummary, "File C");
		assertEquals(0.0, entropy);
	}

	@Test
	public void computeEntropyOfFileInChangeSet_halfOfTotalLinesChangedInFile() {
		changeSetSummary.put("File A", 3);
		changeSetSummary.put("File B", 3);
		changeSetSummary.put("File C", 6);
		final double entropy = entropyComputer.computeEntropyOfFileInChangeSet(changeSetSummary, "File C");
		assertEquals(0.75, entropy);
	}

	@Test
	public void computeEntropyOfFileInChangeSet_quarterOfTotalLinesChangedInFile() {
		changeSetSummary.put("File A", 10);
		changeSetSummary.put("File B", 10);
		changeSetSummary.put("File C", 10);
		changeSetSummary.put("File D", 10);
		final double entropy = entropyComputer.computeEntropyOfFileInChangeSet(changeSetSummary, "File D");
		assertEquals(0.5, entropy);
	}
}
