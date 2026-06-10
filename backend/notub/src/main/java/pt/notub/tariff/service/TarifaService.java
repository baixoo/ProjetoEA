package pt.notub.tariff.service;

import org.springframework.stereotype.Service;
import pt.notub.common.exception.PedidoInvalidoException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.tariff.dto.TarifaDTO;
import pt.notub.tariff.dto.TarifaRequest;
import pt.notub.tariff.entity.ModalidadePasse;
import pt.notub.tariff.entity.Tarifa;
import pt.notub.tariff.mapper.TarifaMapper;
import pt.notub.tariff.repository.TarifaRepository;
import pt.notub.user.entity.TipoUtilizador;

import java.util.List;

@Service
public class TarifaService {

    private static final int MAX_ZONE_NUM = 3;

    private final TarifaRepository tarifaRepository;

    public TarifaService(TarifaRepository tarifaRepository) {
        this.tarifaRepository = tarifaRepository;
    }

    public List<TarifaDTO> getAllTarifas() {
        return TarifaMapper.toDTOList(tarifaRepository.findAll());
    }

    public TarifaDTO getTarifaById(Long id) {
        return TarifaMapper.toDTO(tarifaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarifa nao encontrada")));
    }

    public List<TarifaDTO> getTarifasByTipoUtilizador(String tipoUtilizador) {
        return TarifaMapper.toDTOList(tarifaRepository.findByTipoUtilizador(parseTipoUtilizador(tipoUtilizador)));
    }

    public List<TarifaDTO> getTarifasByModalidade(String modalidade) {
        return TarifaMapper.toDTOList(tarifaRepository.findByModalidade(parseModalidade(modalidade)));
    }

    public TarifaDTO getTarifaByTipoUtilizadorAndModalidade(String tipoUtilizador, String modalidade) {
        return TarifaMapper.toDTO(tarifaRepository
                .findByCriteria(parseTipoUtilizador(tipoUtilizador), parseModalidade(modalidade), 0)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarifa nao encontrada")));
    }

    public TarifaDTO calcularTarifa(String tipoProduto, String tipoUtilizador, String modalidade, int nrZonas) {
        TipoUtilizador tipo = tipoUtilizador != null ? parseTipoUtilizador(tipoUtilizador) : TipoUtilizador.ADULTO;
        String tipoProdutoNormalizado = tipoProduto != null ? tipoProduto.trim().toUpperCase() : null;
        int nrZonasNormalizado = requireNrZonas(nrZonas);
        ModalidadePasse mod = null;
        if (tipoProdutoNormalizado != null && !tipoProdutoNormalizado.equals("BILHETE")
                && !tipoProdutoNormalizado.equals("PASSE")) {
            throw new PedidoInvalidoException("Tipo de produto invalido");
        }
        if (tipoProdutoNormalizado == null || tipoProdutoNormalizado.equals("BILHETE")) {
            if (modalidade != null && !modalidade.isBlank()) {
                throw new PedidoInvalidoException("Modalidade nao aplicavel a bilhete");
            }
        } else {
            mod = parseModalidade(modalidade);
        }
        return TarifaMapper.toDTO(tarifaRepository.findByCriteria(tipo, mod, nrZonasNormalizado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarifa nao encontrada")));
    }

    public TarifaDTO createTarifa(TarifaRequest pedido) {
        Tarifa tarifa = new Tarifa();
        tarifa.setValor(requireValue(pedido.valor(), "Valor invalido"));
        tarifa.setNrZonas(requireNrZonas(pedido.nrZonas()));
        if (pedido.tipoUtilizador() != null) tarifa.setTipoUtilizador(parseTipoUtilizador(pedido.tipoUtilizador()));
        if (pedido.modalidade() != null) tarifa.setModalidade(parseModalidade(pedido.modalidade()));
        return TarifaMapper.toDTO(tarifaRepository.save(tarifa));
    }

    public TarifaDTO updateTarifa(Long id, TarifaRequest updated) {
        Tarifa tarifa = tarifaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tarifa nao encontrada"));
        if (updated.valor() != null) tarifa.setValor(updated.valor());
        if (updated.tipoUtilizador() != null) tarifa.setTipoUtilizador(parseTipoUtilizador(updated.tipoUtilizador()));
        if (updated.modalidade() != null) tarifa.setModalidade(parseModalidade(updated.modalidade()));
        if (updated.nrZonas() != null) tarifa.setNrZonas(requireNrZonas(updated.nrZonas()));
        return TarifaMapper.toDTO(tarifaRepository.save(tarifa));
    }

    public void deleteTarifa(Long id) {
        tarifaRepository.deleteById(id);
    }

    private TipoUtilizador parseTipoUtilizador(String tipoUtilizador) {
        if (tipoUtilizador == null || tipoUtilizador.isBlank()) {
            throw new PedidoInvalidoException("Tipo de utilizador invalido");
        }
        try {
            return TipoUtilizador.valueOf(tipoUtilizador.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PedidoInvalidoException("Tipo de utilizador invalido");
        }
    }

    private ModalidadePasse parseModalidade(String modalidade) {
        if (modalidade == null || modalidade.isBlank()) {
            throw new PedidoInvalidoException("Modalidade invalida");
        }
        try {
            return ModalidadePasse.valueOf(modalidade.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PedidoInvalidoException("Modalidade invalida");
        }
    }

    private Float requireValue(Float value, String message) {
        if (value == null) {
            throw new PedidoInvalidoException(message);
        }
        return value;
    }

    private int requireNrZonas(Integer value) {
        if (value == null || value < 1 || value > MAX_ZONE_NUM) {
            throw new PedidoInvalidoException("Numero de zonas invalido");
        }
        return value;
    }
}
