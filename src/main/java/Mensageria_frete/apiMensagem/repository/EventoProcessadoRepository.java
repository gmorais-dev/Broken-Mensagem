package Mensageria_frete.apiMensagem.repository;

import Mensageria_frete.apiMensagem.entity.EventoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, Long> {

	Optional<EventoProcessado> findByChaveIdempotencia(String chaveIdempotencia);
}
