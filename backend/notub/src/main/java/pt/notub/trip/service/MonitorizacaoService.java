package pt.notub.trip.service;

import java.util.List;
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
    public void onLocationUpdate(Long viagemId, Long novaParagemId) {
        
        List<Utilizador> utilizadores = monitorizacaoRepository.getSubscribedUsers(viagemId);

        if (!utilizadores.isEmpty()) {
            
            String topicoItem = "/topic/viagem/" + viagemId;
            
            messagingTemplate.convertAndSend(topicoItem, "Nova Paragem: " + novaParagemId);
            
            for (Utilizador u : utilizadores) {
                System.out.println("O utilizador " + u.getPrimeiroNome() + u.getUltimoNome() + " acabou de receber a atualização.");
            }
        }
    }

    @Override
    public void onTripFinished(Long viagemId) {
        List<Utilizador> utilizadores = monitorizacaoRepository.getSubscribedUsers(viagemId);

        if (!utilizadores.isEmpty()) {
            String topicoItem = "/topic/viagem/" + viagemId;
            messagingTemplate.convertAndSend(topicoItem, "Viagem Finalizada");
            
            for (Utilizador u : utilizadores) {
                System.out.println("O utilizador " + u.getPrimeiroNome() + u.getUltimoNome() + " acabou de receber a notificação de viagem finalizada.");
            }
        }
    }

}