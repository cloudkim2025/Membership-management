package com.example.memberservice.service;

import com.example.memberservice.domain.QueryHistory;
import com.example.memberservice.repository.QueryHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Service
public class MemberService {
    private final WebClient webClient;
    private final QueryHistoryRepository historyRepository;

    // info-service URL은 환경변수 또는 application.yml 등에서 주입받도록 처리
    private final String infoServiceUrl = System.getenv("INFO_SERVICE_URL") != null
            ? System.getenv("INFO_SERVICE_URL")
            : "http://localhost:8888";

    public MemberService(WebClient webClient, QueryHistoryRepository historyRepository) {
        this.webClient = webClient;
        this.historyRepository = historyRepository;
    }

    public Mono<String> createMember(Object memberData) {
        return webClient.post()
                .uri(infoServiceUrl + "/members")
                .bodyValue(memberData)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response ->
                        saveHistory("INSERT", true, "Created member successfully")
                                .thenReturn(response)
                )
                .onErrorResume(e ->
                        saveHistory("INSERT", false, e.getMessage())
                                .then(Mono.error(e))
                );
    }

    public Mono<String> getMember(Long id) {
        return webClient.get()
                .uri(infoServiceUrl + "/members/" + id)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response ->
                        saveHistory("SELECT", true, "Retrieved member with id: " + id)
                                .thenReturn(response)
                )
                .onErrorResume(e ->
                        saveHistory("SELECT", false, e.getMessage())
                                .then(Mono.error(e))
                );
    }


    public Mono<String> updateMember(Long id, Object memberData) {
        return webClient.put()
                .uri(infoServiceUrl + "/members/" + id)
                .bodyValue(memberData)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response ->
                        saveHistory("UPDATE", true, "Updated member with id: " + id)
                                .thenReturn(response)
                )
                .onErrorResume(e ->
                        saveHistory("UPDATE", false, e.getMessage())
                                .then(Mono.error(e))
                );
    }

    public Mono<String> deleteMember(Long id) {
        return webClient.delete()
                .uri(infoServiceUrl + "/members/" + id)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response ->
                        saveHistory("DELETE", true, "Deleted member with id: " + id)
                                .thenReturn(response)
                )
                .onErrorResume(e ->
                        saveHistory("DELETE", false, e.getMessage())
                                .then(Mono.error(e))
                );
    }

    private Mono<Void> saveHistory(String operation, boolean success, String details) {
        QueryHistory history = new QueryHistory();
        history.setOperation(operation);
        history.setSuccess(success);
        history.setTimestamp(LocalDateTime.now());
        history.setDetails(details);
        return historyRepository.save(history).then();
    }
}
