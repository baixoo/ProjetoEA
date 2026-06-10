package pt.notub.vehicle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.notub.vehicle.entity.Veiculo;

import java.util.List;
import java.util.Optional;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {
    Optional<Veiculo> findByMatricula(String matricula);
    List<Veiculo> findByLinhaIsNotNull();
}
