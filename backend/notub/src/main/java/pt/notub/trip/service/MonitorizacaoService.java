package pt.notub.trip.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.trip.dto.ViagemVeiculoDTO;
import pt.notub.trip.entity.Monitorizacao;
import pt.notub.trip.entity.ViagemVeiculo;
import pt.notub.trip.mapper.ViagemMapper;

import pt.notub.trip.repository.MonitorizacaoRepository;
import pt.notub.trip.repository.ViagemVeiculoRepository;

import pt.notub.user.entity.Utilizador;
import pt.notub.trip.observerPattern.VehicleTripObserver;

@Service
public class MonitorizacaoService implements VehicleTripObserver {

    @Autowired
    private MonitorizacaoRepository monitorizacaoRepository;

    @Autowired
    private ViagemVeiculoRepository viagemVeiculoRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate; 

    private ViagemVeiculo findViagemVeiculo(Long id) {
        return viagemVeiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("ViagemVeiculo nao encontrado"));
    }

    @Override
    public ViagemVeiculoDTO getViagemVeiculoById(Long id) {
        return ViagemMapper.toVeiculoDTO(findViagemVeiculo(id));
    }

    @Override
    public void onLocationUpdate(Long viagemId, Long novoPontoPassagemId) {
        
        List<Utilizador> utilizadores = monitorizacaoRepository.getSubscribedUsers(viagemId);

        if (!utilizadores.isEmpty()) {
            String topicoItem = "/topic/bus." + viagemId + ".route";
            System.out.println("Enviando atualização para o tópico: " + topicoItem);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("pontoAtualId", novoPontoPassagemId);
            
            System.out.println("Payload a ser enviado: " + payload);

            messagingTemplate.convertAndSend(topicoItem, (Object) payload);
                
        }
    }

    @Override
    public void onTripFinished(Long viagemId) {
        List<Utilizador> utilizadores = monitorizacaoRepository.getSubscribedUsers(viagemId);

        if (!utilizadores.isEmpty()) {
            String topicoItem = "/topic/bus." + viagemId + ".route";

            Map<String, Object> payload = new HashMap<>();
            payload.put("viagemStatus", "END");
            
            System.out.println("Payload a ser enviado: " + payload);

            messagingTemplate.convertAndSend(topicoItem, (Object) payload);
            
        }
    }
}