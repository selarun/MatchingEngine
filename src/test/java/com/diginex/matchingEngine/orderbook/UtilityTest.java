package com.diginex.matchingEngine.orderbook;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public interface UtilityTest {

	public default boolean compareTwoFiles(Path exceptedActualOutput, Path actualOutput) {
		try {
			List<String> exceptedActualOutputList = Files.readAllLines(exceptedActualOutput);
			List<String> actualOutputList = Files.readAllLines(actualOutput);
			for (String ex : exceptedActualOutputList) {
				if (ex.trim().isEmpty())
					continue;
				assertThat(actualOutputList, hasItem(ex));
			}

		} catch (IOException ie) {
			fail("File can be accessed:" + ie.getMessage());
		}
		return true;
	}
}