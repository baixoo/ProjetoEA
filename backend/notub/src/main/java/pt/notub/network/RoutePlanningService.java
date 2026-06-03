package pt.notub.network;

import org.springframework.stereotype.Service;
import pt.notub.network.dto.RotaDTO;
import pt.notub.models.*;
import pt.notub.repositories.PontosDePassagemRepository;
import pt.notub.repositories.TrajetoRepository;

import java.util.*;

@Service
public class RoutePlanningService {

    private final PontosDePassagemRepository pontosDePassagemRepository;
    private final TrajetoRepository trajetoRepository;

    public RoutePlanningService(PontosDePassagemRepository pontosDePassagemRepository,
                                TrajetoRepository trajetoRepository) {
        this.pontosDePassagemRepository = pontosDePassagemRepository;
        this.trajetoRepository = trajetoRepository;
    }

    public RotaDTO planearRota(Long origemId, Long destinoId) {
        if (origemId.equals(destinoId)) {
            return new RotaDTO(0, 0, List.of());
        }

        List<PontosDePassagem> allPontos = pontosDePassagemRepository.findAll();
        List<Trajeto> allTrajetos = trajetoRepository.findAll();

        Map<Long, List<PontosDePassagem>> paragemToPontos = new HashMap<>();
        for (PontosDePassagem p : allPontos) {
            if (p.getParagem() != null) {
                paragemToPontos.computeIfAbsent(p.getParagem().getId(), k -> new ArrayList<>()).add(p);
            }
        }

        Map<Long, Trajeto> trajetoCache = new HashMap<>();
        Map<Long, Long> pontoToTrajeto = new HashMap<>();
        for (Trajeto t : allTrajetos) {
            trajetoCache.put(t.getId(), t);
            if (t.getPontosDePassagem() != null) {
                for (PontosDePassagem p : t.getPontosDePassagem()) {
                    pontoToTrajeto.put(p.getId(), t.getId());
                }
            }
        }

        Map<String, List<PontosDePassagem>> trajetoPontosMap = new HashMap<>();
        for (Trajeto t : allTrajetos) {
            if (t.getPontosDePassagem() != null) {
                trajetoPontosMap.put(String.valueOf(t.getId()), new ArrayList<>(t.getPontosDePassagem()));
            }
        }
        for (List<PontosDePassagem> pontos : trajetoPontosMap.values()) {
            pontos.sort(Comparator.comparingInt(PontosDePassagem::getOrdem));
        }

        Map<Long, String> bestLinha = new HashMap<>();
        Map<Long, Long> cameFrom = new HashMap<>();
        Map<Long, Integer> bestCost = new HashMap<>();
        Map<Long, Long> cameFromTrajeto = new HashMap<>();

        PriorityQueue<long[]> queue = new PriorityQueue<>(Comparator.comparingInt(a -> (int) a[1]));
        bestCost.put(origemId, 0);
        queue.add(new long[]{origemId, 0});

        while (!queue.isEmpty()) {
            long[] current = queue.poll();
            long currentParagemId = current[0];
            int currentCost = (int) current[1];

            if (currentParagemId == destinoId) break;
            if (currentCost > bestCost.getOrDefault(currentParagemId, Integer.MAX_VALUE)) continue;

            List<PontosDePassagem> pontosHere = paragemToPontos.getOrDefault(currentParagemId, List.of());

            for (PontosDePassagem ponto : pontosHere) {
                Long trajetoId = pontoToTrajeto.get(ponto.getId());
                if (trajetoId == null) continue;
                List<PontosDePassagem> trajetoPontos = trajetoPontosMap.get(String.valueOf(trajetoId));

                int currentIdx = -1;
                for (int i = 0; i < trajetoPontos.size(); i++) {
                    if (trajetoPontos.get(i).getParagem().getId().equals(currentParagemId)) {
                        currentIdx = i;
                        break;
                    }
                }
                if (currentIdx < 0) continue;

                for (int i = currentIdx + 1; i < trajetoPontos.size(); i++) {
                    PontosDePassagem next = trajetoPontos.get(i);
                    Long nextParagemId = next.getParagem().getId();
                    int timeDelta = next.getTempoDesdeInicio() - ponto.getTempoDesdeInicio();
                    if (timeDelta <= 0) timeDelta = 2;

                    int newCost = currentCost + timeDelta;

                    if (newCost < bestCost.getOrDefault(nextParagemId, Integer.MAX_VALUE)) {
                        bestCost.put(nextParagemId, newCost);
                        cameFrom.put(nextParagemId, currentParagemId);
                        bestLinha.put(nextParagemId, String.valueOf(trajetoId));
                        cameFromTrajeto.put(nextParagemId, trajetoId);
                        queue.add(new long[]{nextParagemId, newCost});
                    }
                }
            }
        }

        if (!cameFrom.containsKey(destinoId) && !origemId.equals(destinoId)) {
            return null;
        }

        List<Long> path = new ArrayList<>();
        Long current = destinoId;
        while (current != null) {
            path.add(0, current);
            current = cameFrom.get(current);
        }

        Map<Long, Paragem> paragemCache = new HashMap<>();
        for (PontosDePassagem p : allPontos) {
            if (p.getParagem() != null) {
                paragemCache.putIfAbsent(p.getParagem().getId(), p.getParagem());
            }
        }

        List<RotaDTO.SegmentoDTO> segmentos = new ArrayList<>();
        Long currentTrajetoId = cameFromTrajeto.get(path.size() > 1 ? path.get(1) : null);
        int segStart = 0;

        for (int i = 1; i < path.size(); i++) {
            Long nextTrajetoId = cameFromTrajeto.get(path.get(i));
            if (nextTrajetoId == null) nextTrajetoId = currentTrajetoId;

            if (!nextTrajetoId.equals(currentTrajetoId) || i == path.size() - 1) {
                int endIdx = (i == path.size() - 1 && nextTrajetoId.equals(currentTrajetoId)) ? i : i - 1;
                if (i == path.size() - 1 && !nextTrajetoId.equals(currentTrajetoId)) {
                    endIdx = i - 1;
                } else if (i == path.size() - 1) {
                    endIdx = i;
                }

                RotaDTO.SegmentoDTO seg = new RotaDTO.SegmentoDTO();
                Trajeto trajeto = trajetoCache.get(currentTrajetoId);
                if (trajeto != null) {
                    seg.setLinhaNome(trajeto.getLinha() != null ? trajeto.getLinha().getNome() : "Linha");
                    seg.setDirecao(trajeto.getDirecao() != null ? trajeto.getDirecao().name() : "");
                }

                Paragem origem = paragemCache.get(path.get(segStart));
                Paragem destino = paragemCache.get(path.get(endIdx));
                seg.setOrigem(new RotaDTO.ParagemDTO(origem.getId(), origem.getNome()));
                seg.setDestino(new RotaDTO.ParagemDTO(destino.getId(), destino.getNome()));

                List<RotaDTO.ParagemDTO> paragens = new ArrayList<>();
                for (int j = segStart; j <= endIdx; j++) {
                    Paragem p = paragemCache.get(path.get(j));
                    paragens.add(new RotaDTO.ParagemDTO(p.getId(), p.getNome()));
                }
                seg.setParagens(paragens);

                List<PontosDePassagem> trajetoPontos = trajetoPontosMap.get(String.valueOf(currentTrajetoId));
                int startTime = 0, endTime = 0;
                if (trajetoPontos != null) {
                    for (PontosDePassagem pp : trajetoPontos) {
                        if (pp.getParagem().getId().equals(path.get(segStart))) startTime = pp.getTempoDesdeInicio();
                        if (pp.getParagem().getId().equals(path.get(endIdx))) endTime = pp.getTempoDesdeInicio();
                    }
                }
                seg.setDuracaoMinutos(Math.max(1, endTime - startTime));
                segmentos.add(seg);

                segStart = i;
                currentTrajetoId = nextTrajetoId;
            }
        }

        int totalMinutos = segmentos.stream().mapToInt(RotaDTO.SegmentoDTO::getDuracaoMinutos).sum();
        int trocas = Math.max(0, segmentos.size() - 1);

        return new RotaDTO(totalMinutos, trocas, segmentos);
    }
}
