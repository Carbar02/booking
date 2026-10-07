package it.eng.booking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import it.eng.booking.controller.BookingController;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:booking-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BookingApplicationTests {

	@Autowired
	private BookingController bookingController;

	@Test
	void contextLoads() {
	}

	@Test
	void getCamereReturnsJson() throws Exception {
		MockMvcBuilders.standaloneSetup(bookingController).build()
				.perform(get("/camere"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(5))
				.andExpect(jsonPath("$[0].numero").value("101"))
				.andExpect(jsonPath("$[0].tipo").value("SINGOLA"))
				.andExpect(jsonPath("$[0].appartamentoNome").value("Aurora"));
	}

}
