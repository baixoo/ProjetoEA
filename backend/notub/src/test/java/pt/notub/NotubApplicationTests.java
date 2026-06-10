package pt.notub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotubApplicationTests {

	@Test
	void applicationClassCanBeLoaded() {
		assertDoesNotThrow(() -> Class.forName(NotubApplication.class.getName()));
	}

}
