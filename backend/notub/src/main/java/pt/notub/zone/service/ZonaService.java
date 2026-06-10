package pt.notub.zone.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.network.entity.Paragem;
import pt.notub.network.repository.ParagemRepository;
import pt.notub.zone.dto.ZonaDTO;
import pt.notub.zone.dto.ZonaRequest;
import pt.notub.zone.entity.Zona;
import pt.notub.zone.mapper.ZonaMapper;
import pt.notub.zone.repository.ZonaRepository;

import java.util.Comparator;
import java.util.List;

@Service
public class ZonaService {

    private static final int MAX_ZONE_NUM = 3;

    private final ZonaRepository zonaRepository;
    private final ParagemRepository paragemRepository;

    public ZonaService(ZonaRepository zonaRepository, ParagemRepository paragemRepository) {
        this.zonaRepository = zonaRepository;
        this.paragemRepository = paragemRepository;
    }

    public List<ZonaDTO> getAllZonas() {
        return ZonaMapper.toDTOList(zonaRepository.findAll().stream()
                .sorted(Comparator.comparing(Zona::getNum, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Zona::getId, Comparator.nullsLast(Long::compareTo)))
                .toList());
    }

    public ZonaDTO getZonaById(Long id) {
        Zona zona = zonaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Zona nao encontrada"));
        return ZonaMapper.toDTO(zona);
    }

    public ZonaDTO getZonaByNome(String nome) {
        Zona zona = zonaRepository.findByNome(nome)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Zona nao encontrada"));
        return ZonaMapper.toDTO(zona);
    }

    public ZonaDTO createZona(ZonaRequest pedido) {
        Zona zona = new Zona();
        zona.setNome(pedido.nome());
        zona.setNum(requireSupportedZoneNum(pedido.num()));
        return ZonaMapper.toDTO(zonaRepository.save(zona));
    }

    public ZonaDTO updateZona(Long id, ZonaRequest updated) {
        Zona zona = zonaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Zona nao encontrada"));
        if (updated.nome() != null) zona.setNome(updated.nome());
        if (updated.num() != null) zona.setNum(requireSupportedZoneNum(updated.num()));
        requireSupportedZoneNum(zona.getNum());
        return ZonaMapper.toDTO(zonaRepository.save(zona));
    }

    public void deleteZona(Long id) {
        zonaRepository.deleteById(id);
    }

    public ZonaDTO addParagem(Long zonaId, Long paragemId) {
        Zona zona = zonaRepository.findById(zonaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Zona nao encontrada"));
        requireSupportedZoneNum(zona.getNum());
        Paragem paragem = paragemRepository.findById(paragemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));
        paragem.setZona(zona);
        paragemRepository.save(paragem);
        return ZonaMapper.toDTO(zona);
    }

    public void removeParagem(Long paragemId) {
        Paragem paragem = paragemRepository.findById(paragemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Paragem nao encontrada"));
        paragem.setZona(null);
        paragemRepository.save(paragem);
    }

    private int requireSupportedZoneNum(Integer num) {
        if (num == null || num < 1 || num > MAX_ZONE_NUM) {
            throw new PedidoInvalidoException("Numero de zona invalido");
        }
        return num;
    }
}
