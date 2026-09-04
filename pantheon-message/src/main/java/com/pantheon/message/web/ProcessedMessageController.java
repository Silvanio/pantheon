package com.pantheon.message.web;

import com.pantheon.message.domain.ProcessedMessageRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class ProcessedMessageController {

    private final ProcessedMessageRepository repository;

    public ProcessedMessageController(ProcessedMessageRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ProcessedMessageResponse> list() {
        return repository.findAll().stream().map(ProcessedMessageResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessedMessageResponse> get(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ProcessedMessageResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
