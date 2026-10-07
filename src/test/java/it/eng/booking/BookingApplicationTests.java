package it.eng.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import it.eng.booking.controller.BookingController;
import it.eng.booking.dto.CameraPrenotazioneRequest;
import it.eng.booking.dto.CreaPrenotazioneRequest;
import it.eng.booking.dto.OspiteRequest;
import it.eng.booking.dto.PrenotazioneResponse;
import it.eng.booking.exception.CameraNonDisponibileException;
import it.eng.booking.exception.GlobalExceptionHandler;
import it.eng.booking.model.Camera;
import it.eng.booking.model.StatoPrenotazione;
import it.eng.booking.repository.CameraRepository;
import it.eng.booking.repository.OspiteRepository;
import it.eng.booking.repository.PrenotazioneRepository;
import it.eng.booking.service.PrenotazioneService;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:booking-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BookingApplicationTests {

	@Autowired
	private BookingController bookingController;

	@Autowired
	private PrenotazioneService prenotazioneService;

	@Autowired
	private PrenotazioneRepository prenotazioneRepository;

	@Autowired
	private OspiteRepository ospiteRepository;

	@Autowired
	private CameraRepository cameraRepository;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private MockMvc mockMvc;
	private List<Camera> camere;
	private final LocalDate arrivo = LocalDate.now().plusDays(30);

	@BeforeEach
	void setUp() {
		prenotazioneRepository.deleteAll();
		ospiteRepository.deleteAll();
		camere = cameraRepository.findAllByOrderByIdAsc();
		mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
				.setControllerAdvice(new GlobalExceptionHandler()).build();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void getCamereReturnsJson() throws Exception {
		mockMvc.perform(get("/camere"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(5))
				.andExpect(jsonPath("$[0].numero").value("101"))
				.andExpect(jsonPath("$[0].tipo").value("SINGOLA"))
				.andExpect(jsonPath("$[0].appartamentoNome").value("Aurora"));
	}

	@Test
	void getAppartamentiIncludesCamere() throws Exception {
		mockMvc.perform(get("/appartamenti"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].camere.length()").value(3));
	}

	@Test
	void getCamereFiltersByAppartamentoAndTipo() throws Exception {
		mockMvc.perform(get("/camere").param("tipo", "DOPPIA")
				.param("appartamentoId", camere.get(0).getAppartamento().getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].numero").value("102"));
	}

	@Test
	void disponibiliExcludeInactiveAndInsufficientCapacity() throws Exception {
		mockMvc.perform(get("/camere/disponibili")
				.param("dataArrivo", arrivo.toString())
				.param("dataPartenza", arrivo.plusDays(2).toString())
				.param("numeroOspiti", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3));
	}

	@Test
	void creaPrenotazioneReturnsCreatedAndLocation() throws Exception {
		mockMvc.perform(post("/prenotazioni").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(richiesta(camere.get(0).getId(), 1, arrivo))))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.stato").value("CONFERMATA"))
				.andExpect(jsonPath("$.totale").value(110.0));
	}

	@Test
	void overlappingBookingReturnsConflict() throws Exception {
		prenotazioneService.crea(richiesta(camere.get(0).getId(), 1, arrivo));
		mockMvc.perform(post("/prenotazioni").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(richiesta(camere.get(0).getId(), 1, arrivo.plusDays(1)))))
				.andExpect(status().isConflict());
		assertEquals(1, prenotazioneRepository.count());
		assertEquals(1, ospiteRepository.count());
	}

	@Test
	void checkInOnCheckOutDateIsAllowed() {
		prenotazioneService.crea(richiesta(camere.get(0).getId(), 1, arrivo));
		prenotazioneService.crea(richiesta(camere.get(0).getId(), 1, arrivo.plusDays(2)));
		assertEquals(2, prenotazioneRepository.count());
	}

	@Test
	void cancellationPreservesHistoryAndReleasesCamera() throws Exception {
		PrenotazioneResponse prenotazione = prenotazioneService.crea(richiesta(camere.get(0).getId(), 1, arrivo));
		mockMvc.perform(get("/camere/disponibili")
				.param("dataArrivo", arrivo.toString()).param("dataPartenza", arrivo.plusDays(2).toString()))
				.andExpect(jsonPath("$.length()").value(3));
		mockMvc.perform(post("/prenotazioni/" + prenotazione.id() + "/annulla"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.stato").value("ANNULLATA"));
		mockMvc.perform(get("/camere/disponibili")
				.param("dataArrivo", arrivo.toString()).param("dataPartenza", arrivo.plusDays(2).toString()))
				.andExpect(jsonPath("$.length()").value(4));
		assertEquals(StatoPrenotazione.ANNULLATA, prenotazioneService.getPrenotazione(prenotazione.id()).stato());
		prenotazioneService.crea(richiesta(camere.get(0).getId(), 1, arrivo));
		assertEquals(2, prenotazioneRepository.count());
	}

	@Test
	void multiRoomBookingCalculatesTotal() {
		CreaPrenotazioneRequest request = new CreaPrenotazioneRequest(ospite(), arrivo, arrivo.plusDays(2),
				List.of(new CameraPrenotazioneRequest(camere.get(0).getId(), 1),
						new CameraPrenotazioneRequest(camere.get(1).getId(), 2)));
		PrenotazioneResponse response = prenotazioneService.crea(request);
		assertEquals(2, response.camere().size());
		assertEquals(0, new BigDecimal("280.00").compareTo(response.totale()));
	}

	@Test
	void multiRoomBookingIsAtomicWhenOneRoomIsOccupied() {
		prenotazioneService.crea(richiesta(camere.get(1).getId(), 2, arrivo));
		CreaPrenotazioneRequest request = new CreaPrenotazioneRequest(ospite(), arrivo, arrivo.plusDays(2),
				List.of(new CameraPrenotazioneRequest(camere.get(0).getId(), 1),
						new CameraPrenotazioneRequest(camere.get(1).getId(), 2)));
		assertThrows(CameraNonDisponibileException.class, () -> prenotazioneService.crea(request));
		assertEquals(1, prenotazioneRepository.count());
		assertEquals(1, ospiteRepository.count());
		prenotazioneService.crea(richiesta(camere.get(0).getId(), 1, arrivo));
		assertEquals(2, prenotazioneRepository.count());
	}

	@Test
	void agreedPriceDoesNotChangeWithRoomPrice() {
		Camera camera = camere.get(0);
		PrenotazioneResponse prenotazione = prenotazioneService.crea(richiesta(camera.getId(), 1, arrivo));
		try {
			jdbcTemplate.update("update camera set prezzo_per_notte = ? where id = ?", new BigDecimal("75.00"), camera.getId());
			assertEquals(0, new BigDecimal("110.00").compareTo(
					prenotazioneService.getPrenotazione(prenotazione.id()).totale()));
		} finally {
			jdbcTemplate.update("update camera set prezzo_per_notte = ? where id = ?", camera.getPrezzoPerNotte(), camera.getId());
		}
	}

	@Test
	void invalidDatesReturnBadRequest() throws Exception {
		CreaPrenotazioneRequest request = new CreaPrenotazioneRequest(ospite(), arrivo, arrivo,
				List.of(new CameraPrenotazioneRequest(camere.get(0).getId(), 1)));
		mockMvc.perform(post("/prenotazioni").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/camere/disponibili")
				.param("dataArrivo", arrivo.toString()).param("dataPartenza", arrivo.toString()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void invalidGuestIsRejectedByValidation() throws Exception {
		CreaPrenotazioneRequest request = new CreaPrenotazioneRequest(
				new OspiteRequest("", "Rossi", "non-valida", ""), arrivo, arrivo.plusDays(2),
				List.of(new CameraPrenotazioneRequest(camere.get(0).getId(), 1)));
		mockMvc.perform(post("/prenotazioni").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errori").isArray());
		assertEquals(0, ospiteRepository.count());
	}

	@Test
	void exceedingCapacityAndDuplicateRoomsReturnBadRequest() throws Exception {
		mockMvc.perform(post("/prenotazioni").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(richiesta(camere.get(0).getId(), 2, arrivo))))
				.andExpect(status().isBadRequest());
		CameraPrenotazioneRequest camera = new CameraPrenotazioneRequest(camere.get(0).getId(), 1);
		CreaPrenotazioneRequest duplicata = new CreaPrenotazioneRequest(ospite(), arrivo, arrivo.plusDays(2),
				List.of(camera, camera));
		mockMvc.perform(post("/prenotazioni").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(duplicata)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void inactiveRoomReturnsConflict() throws Exception {
		mockMvc.perform(post("/prenotazioni").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(richiesta(camere.get(2).getId(), 1, arrivo))))
				.andExpect(status().isConflict());
	}

	@Test
	void missingResourcesReturnNotFound() throws Exception {
		mockMvc.perform(get("/prenotazioni/999999"))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/prenotazioni").contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(richiesta(999999L, 1, arrivo))))
				.andExpect(status().isNotFound());
	}

	private OspiteRequest ospite() {
		return new OspiteRequest("Mario", "Rossi", "mario@example.com", "+39 3331234567");
	}

	private CreaPrenotazioneRequest richiesta(Long cameraId, int numeroOspiti, LocalDate dataArrivo) {
		return new CreaPrenotazioneRequest(ospite(), dataArrivo, dataArrivo.plusDays(2),
				List.of(new CameraPrenotazioneRequest(cameraId, numeroOspiti)));
	}

}
