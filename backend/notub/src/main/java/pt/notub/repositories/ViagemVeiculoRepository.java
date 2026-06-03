package pt.notub.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.models.ViagemVeiculo;

import java.util.List;

@Repository
public interface ViagemVeiculoRepository extends JpaRepository<ViagemVeiculo, Long> {
    List<ViagemVeiculo> findByVeiculoId(Long veiculoId);
    List<ViagemVeiculo> findByTrajetoId(Long trajetoId);
}
