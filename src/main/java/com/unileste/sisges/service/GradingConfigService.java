package com.unileste.sisges.service;

import com.unileste.sisges.controller.dto.grading.GradingConfigResponse;
import com.unileste.sisges.controller.dto.grading.UpdateGradingConfigRequest;
import com.unileste.sisges.exception.BusinessRuleException;
import com.unileste.sisges.model.GradingConfig;
import com.unileste.sisges.repository.GradingConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GradingConfigService {

    private final GradingConfigRepository repository;

    @Transactional(readOnly = true)
    public GradingConfigResponse get() {
        GradingConfig cfg = repository.findFirstByOrderByIdAsc()
                .orElseGet(this::buildDefault);
        return toResponse(cfg);
    }

    @Transactional
    public GradingConfigResponse update(UpdateGradingConfigRequest request) {
        int trimesterSum = request.getTrimester1MaxPoints()
                + request.getTrimester2MaxPoints()
                + request.getTrimester3MaxPoints();
        if (trimesterSum != request.getYearMaxPoints()) {
            throw new BusinessRuleException(
                    "A soma dos pontos dos trimestres (" + trimesterSum
                            + ") deve ser igual ao total do ano letivo (" + request.getYearMaxPoints() + ").");
        }

        assertCompositionMatchesMax(
                "1\u00BA trimestre",
                request.getTrimester1MaxPoints(),
                request.getTrimester1PointsProvas(),
                request.getTrimester1PointsAtividades(),
                request.getTrimester1PointsTrabalhos());
        assertCompositionMatchesMax(
                "2\u00BA trimestre",
                request.getTrimester2MaxPoints(),
                request.getTrimester2PointsProvas(),
                request.getTrimester2PointsAtividades(),
                request.getTrimester2PointsTrabalhos());
        assertCompositionMatchesMax(
                "3\u00BA trimestre",
                request.getTrimester3MaxPoints(),
                request.getTrimester3PointsProvas(),
                request.getTrimester3PointsAtividades(),
                request.getTrimester3PointsTrabalhos());

        GradingConfig cfg = repository.findFirstByOrderByIdAsc()
                .orElseGet(() -> repository.save(buildDefault()));

        cfg.setYearMaxPoints(request.getYearMaxPoints());
        cfg.setYearMinPercentage(request.getYearMinPercentage());
        cfg.setTrimester1MaxPoints(request.getTrimester1MaxPoints());
        cfg.setTrimester1MinPercentage(request.getTrimester1MinPercentage());
        cfg.setTrimester2MaxPoints(request.getTrimester2MaxPoints());
        cfg.setTrimester2MinPercentage(request.getTrimester2MinPercentage());
        cfg.setTrimester3MaxPoints(request.getTrimester3MaxPoints());
        cfg.setTrimester3MinPercentage(request.getTrimester3MinPercentage());
        cfg.setTrimester1PointsProvas(request.getTrimester1PointsProvas());
        cfg.setTrimester1PointsAtividades(request.getTrimester1PointsAtividades());
        cfg.setTrimester1PointsTrabalhos(request.getTrimester1PointsTrabalhos());
        cfg.setTrimester2PointsProvas(request.getTrimester2PointsProvas());
        cfg.setTrimester2PointsAtividades(request.getTrimester2PointsAtividades());
        cfg.setTrimester2PointsTrabalhos(request.getTrimester2PointsTrabalhos());
        cfg.setTrimester3PointsProvas(request.getTrimester3PointsProvas());
        cfg.setTrimester3PointsAtividades(request.getTrimester3PointsAtividades());
        cfg.setTrimester3PointsTrabalhos(request.getTrimester3PointsTrabalhos());

        cfg = repository.save(cfg);
        return toResponse(cfg);
    }

    private static void assertCompositionMatchesMax(
            String label,
            int maxPoints,
            int provas,
            int atividades,
            int trabalhos) {
        if (provas < 1 || atividades < 1 || trabalhos < 1) {
            throw new BusinessRuleException(
                    "No " + label + ", provas, atividades e trabalhos devem ser todos maiores que zero.");
        }
        int s = provas + atividades + trabalhos;
        if (s != maxPoints) {
            throw new BusinessRuleException(
                    "No " + label + ", a soma de provas, atividades e trabalhos (" + s
                            + ") deve ser igual \u00e0 pontua\u00e7\u00e3o m\u00e1xima do trimestre (" + maxPoints + ").");
        }
    }

    private static int[] splitTrimesterComposition(int maxPoints) {
        int a = maxPoints / 3;
        int b = maxPoints / 3;
        int c = maxPoints - a - b;
        return new int[]{a, b, c};
    }

    private GradingConfig buildDefault() {
        int[] t1 = splitTrimesterComposition(33);
        int[] t2 = splitTrimesterComposition(33);
        int[] t3 = splitTrimesterComposition(34);
        return GradingConfig.builder()
                .yearMaxPoints(100)
                .yearMinPercentage(new BigDecimal("60.00"))
                .trimester1MaxPoints(33)
                .trimester1MinPercentage(new BigDecimal("60.00"))
                .trimester2MaxPoints(33)
                .trimester2MinPercentage(new BigDecimal("60.00"))
                .trimester3MaxPoints(34)
                .trimester3MinPercentage(new BigDecimal("60.00"))
                .trimester1PointsProvas(t1[0])
                .trimester1PointsAtividades(t1[1])
                .trimester1PointsTrabalhos(t1[2])
                .trimester2PointsProvas(t2[0])
                .trimester2PointsAtividades(t2[1])
                .trimester2PointsTrabalhos(t2[2])
                .trimester3PointsProvas(t3[0])
                .trimester3PointsAtividades(t3[1])
                .trimester3PointsTrabalhos(t3[2])
                .build();
    }

    private GradingConfigResponse toResponse(GradingConfig cfg) {
        return GradingConfigResponse.builder()
                .yearMaxPoints(cfg.getYearMaxPoints())
                .yearMinPercentage(cfg.getYearMinPercentage())
                .trimester1MaxPoints(cfg.getTrimester1MaxPoints())
                .trimester1MinPercentage(cfg.getTrimester1MinPercentage())
                .trimester2MaxPoints(cfg.getTrimester2MaxPoints())
                .trimester2MinPercentage(cfg.getTrimester2MinPercentage())
                .trimester3MaxPoints(cfg.getTrimester3MaxPoints())
                .trimester3MinPercentage(cfg.getTrimester3MinPercentage())
                .trimester1PointsProvas(cfg.getTrimester1PointsProvas())
                .trimester1PointsAtividades(cfg.getTrimester1PointsAtividades())
                .trimester1PointsTrabalhos(cfg.getTrimester1PointsTrabalhos())
                .trimester2PointsProvas(cfg.getTrimester2PointsProvas())
                .trimester2PointsAtividades(cfg.getTrimester2PointsAtividades())
                .trimester2PointsTrabalhos(cfg.getTrimester2PointsTrabalhos())
                .trimester3PointsProvas(cfg.getTrimester3PointsProvas())
                .trimester3PointsAtividades(cfg.getTrimester3PointsAtividades())
                .trimester3PointsTrabalhos(cfg.getTrimester3PointsTrabalhos())
                .build();
    }
}
