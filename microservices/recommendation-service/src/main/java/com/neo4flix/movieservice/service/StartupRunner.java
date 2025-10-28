package com.neo4flix.movieservice.service;

import com.neo4flix.movieservice.service.impl.RecommendationServiceImpl;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupRunner implements CommandLineRunner {

    private final RecommendationServiceImpl recommendationService;

    public StartupRunner(RecommendationServiceImpl recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Override
    public void run(String... args) {

//        List<MovieRecommendationDTO> rawRecommendations = userRepository.getContentBasedRecommendations(
//                request.getUserId()
////                2
////                minMovieRating,
////                request.getLimit()
//        );
//        var a = userRepository.existsById(request.getUserId());
//        logger.info("==============================================================");
//        logger.info("{}---(3d519f62-6c26-409a-bd3d-3cf978ccf5d4) {}", request.getUserId(), a);
//        logger.info("{}", rawRecommendations.size());
//        logger.info("==============================================================");
//        recommendationService.verifyDatabaseAccess();
    }
}
