package kr.hhplus.be.server.payment.infra.persistence;

import jakarta.persistence.EntityManager;
import kr.hhplus.be.server.payment.domain.Payment;
import kr.hhplus.be.server.payment.port.out.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final EntityManager em;

    @Override
    public void save(Payment payment){
        em.persist(payment);
    }
}
