package kr.hhplus.be.server;

import kr.hhplus.be.server.reservation.application.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.Assert.assertNotNull;

@SpringBootTest
class ServerApplicationTests {
	@Autowired
	ReservationService reservationService;

	@Test
	void 의존성이_잘_주입된다() {
		assertNotNull(reservationService);
	}

}
