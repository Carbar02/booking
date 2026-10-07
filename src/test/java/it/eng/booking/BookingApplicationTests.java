package it.eng.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import it.eng.booking.controller.BookingController;

@SpringBootTest
class BookingApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void getCamereReturnsJson() throws Exception {
		MockMvcBuilders.standaloneSetup(new BookingController()).build()
				.perform(get("/camere"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						["Camera singola", "Camera doppia", "Camera familiare"]
						"""));
	}

}
