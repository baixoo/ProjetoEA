package pt.notub.network;

import org.junit.jupiter.api.*;
import pt.notub.models.*;
import pt.notub.repositories.PontosDePassagemRepository;
import pt.notub.repositories.TrajetoRepository;
import pt.notub.repositories.ViagemRepository;
import pt.notub.network.dto.RotaDTO;

import java.io.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RoutePlanningPerformanceTest {

    private static RoutePlanningService service;

    @BeforeAll
    static void setUp() throws Exception {
        PontosDePassagemRepository pontosRepo = mock(PontosDePassagemRepository.class);
        TrajetoRepository trajetoRepo = mock(TrajetoRepository.class);
        ViagemRepository viagemRepo = mock(ViagemRepository.class);

        System.out.println("Parsing data.sql.gz...");
        long t0 = System.currentTimeMillis();

        Pattern paragemPat = Pattern.compile("VALUES \\((\\d+), '((?:[^']|'')*)', ([^,]+), ([^,]+), (\\d+)\\)");
        Pattern linhaPat = Pattern.compile("VALUES \\((\\d+), '((?:[^']|'')*)', '((?:[^']|'')*)'\\)");
        Pattern trajetoPat = Pattern.compile("VALUES \\((\\d+), '(IDA|VOLTA)', (\\d+)\\)");
        Pattern pontosPat = Pattern.compile("VALUES \\((\\d+), (\\d+), '([^']*)', (\\d+), (\\d+), (\\d+)\\)");
        Pattern viagemPat = Pattern.compile("VALUES \\((\\d+), (\\d+), '([^']*)', '([^']*)', '([^']*)'\\)");        Map<Long, Paragem> paragemMap = new HashMap<>();
        Map<Long, Linha> linhaMap = new HashMap<>();
        Map<Long, Trajeto> trajetoMap = new HashMap<>();
        Map<Long, List<PontosDePassagem>> pontosByTrajetoId = new HashMap<>();
        List<PontosDePassagem> allPontos = new ArrayList<>();
        List<Viagem> allViagens = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(RoutePlanningPerformanceTest.class.getResourceAsStream("/data.sql.gz"))))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (lineNum % 5000 == 0) System.out.println("  line " + lineNum + "...");
                if (!line.startsWith("INSERT INTO ")) continue;

                String table = line.substring(12, line.indexOf(' ', 12));

                switch (table) {
                    case "paragem": {
                        Matcher m = paragemPat.matcher(line);
                        if (m.find()) {
                            Paragem p = new Paragem();
                            p.setId(Long.parseLong(m.group(1)));
                            p.setNome(m.group(2).replace("''", "'"));
                            p.setLocalizacao(new Point(Double.parseDouble(m.group(3)), Double.parseDouble(m.group(4))));
                            paragemMap.put(p.getId(), p);
                        }
                        break;
                    }
                    case "linha": {
                        Matcher m = linhaPat.matcher(line);
                        if (m.find()) {
                            Linha l = new Linha();
                            l.setId(Long.parseLong(m.group(1)));
                            l.setNome(m.group(2).replace("''", "'"));
                            l.setIdentificadorServico(m.group(3).replace("''", "'"));
                            linhaMap.put(l.getId(), l);
                        }
                        break;
                    }
                    case "trajeto": {
                        Matcher m = trajetoPat.matcher(line);
                        if (m.find()) {
                            Trajeto t = new Trajeto();
                            t.setId(Long.parseLong(m.group(1)));
                            t.setDirecao(Direcao.valueOf(m.group(2)));
                            t.setLinha(linhaMap.get(Long.parseLong(m.group(3))));
                            trajetoMap.put(t.getId(), t);
                        }
                        break;
                    }
                    case "pontos_de_passagem": {
                        Matcher m = pontosPat.matcher(line);
                        if (m.find()) {
                            PontosDePassagem p = new PontosDePassagem();
                            p.setId(Long.parseLong(m.group(1)));
                            p.setOrdem(Integer.parseInt(m.group(2)));
                            p.setHoraChegada(LocalTime.parse(m.group(3)));
                            p.setTempoDesdeInicio(Integer.parseInt(m.group(4)));
                            long trajetoId = Long.parseLong(m.group(5));
                            long paragemId = Long.parseLong(m.group(6));
                            p.setParagem(paragemMap.get(paragemId));
                            if (p.getParagem() == null) break;
                            allPontos.add(p);
                            pontosByTrajetoId.computeIfAbsent(trajetoId, k -> new ArrayList<>()).add(p);
                        }
                        break;
                    }
                    case "viagem": {
                        Matcher m = viagemPat.matcher(line);
                        if (m.find()) {
                            Viagem v = new Viagem();
                            v.setId(Long.parseLong(m.group(1)));
                            long trajetoId = Long.parseLong(m.group(2));
                            v.setServiceId(m.group(3));
                            v.setHoraPartida(LocalTime.parse(m.group(4)));
                            v.setGtfsTripId(m.group(5));
                            Trajeto t = new Trajeto();
                            t.setId(trajetoId);
                            v.setTrajeto(t);
                            allViagens.add(v);
                        }
                        break;
                    }
                }
            }
        }

        for (var entry : pontosByTrajetoId.entrySet()) {
            Trajeto t = trajetoMap.get(entry.getKey());
            if (t != null) {
                var sorted = new ArrayList<>(entry.getValue());
                sorted.sort(Comparator.comparingInt(PontosDePassagem::getOrdem));
                t.setPontosDePassagem(sorted);
            }
        }

        System.out.println("Parsed in " + (System.currentTimeMillis() - t0) + "ms: " +
                paragemMap.size() + " paragens, " + trajetoMap.size() + " trajetos, " +
                allPontos.size() + " pontos, " + allViagens.size() + " viagens");

        when(pontosRepo.findAll()).thenReturn(allPontos);
        when(trajetoRepo.findAll()).thenReturn(new ArrayList<>(trajetoMap.values()));
        when(viagemRepo.findAll()).thenReturn(allViagens);

        service = new RoutePlanningService(pontosRepo, trajetoRepo, viagemRepo);
    }

    @Test
    @Order(1)
    void testDirectRoute_Campanha_CasteloQueijo() {
        long t0 = System.currentTimeMillis();
        List<RotaDTO> rotas = service.planearRota(546L, 631L, LocalTime.of(8, 0), DayOfWeek.MONDAY);
        long elapsed = System.currentTimeMillis() - t0;

        System.out.printf("%n=== 546→631 08:00 MON: %dms, %d routes ===%n", elapsed, rotas.size());
        for (int i = 0; i < rotas.size(); i++) {
            RotaDTO r = rotas.get(i);
            System.out.printf("  #%d: %dmin, %d trocas, direta=%s, %s→%s%n",
                    i + 1, r.getTotalMinutos(), r.getTrocas(), r.isDireta(), r.getHoraPartida(), r.getHoraChegada());
            if (r.getSegmentos() != null) {
                for (var seg : r.getSegmentos()) {
                    System.out.printf("    %s: %s → %s (%dmin) dest=%s%n",
                            seg.getLinhaNome(), seg.getOrigem().getNome(),
                            seg.getDestino().getNome(), seg.getDuracaoMinutos(), seg.getDestinoFinal());
                }
            }
        }

        assertFalse(rotas.isEmpty(), "Should find at least one route");
        assertTrue(elapsed < 10000, "Should be <10s, took " + elapsed + "ms");
    }

    @Test
    @Order(2)
    void testCached() {
        long t0 = System.currentTimeMillis();
        List<RotaDTO> rotas = service.planearRota(546L, 631L, LocalTime.of(8, 0), DayOfWeek.MONDAY);
        long elapsed = System.currentTimeMillis() - t0;
        System.out.printf("%n=== 546→631 (cached): %dms, %d routes ===%n", elapsed, rotas.size());
        assertTrue(elapsed < 3000, "Cached should be <3s, took " + elapsed + "ms");
    }

    @Test
    @Order(3)
    void testSameOriginDestination() {
        List<RotaDTO> rotas = service.planearRota(546L, 546L, LocalTime.of(8, 0), DayOfWeek.MONDAY);
        assertTrue(rotas.isEmpty());
    }
}
