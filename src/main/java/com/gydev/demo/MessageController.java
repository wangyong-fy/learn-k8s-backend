package com.gydev.demo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MessageController {

    private final MessageRepository repository;

    @Value("${HOSTNAME:unknown}")
    private String hostname;

    public MessageController(MessageRepository repository) {
        this.repository = repository;
    }

    /** 查：全部 */
    @GetMapping("/messages")
    public List<Message> list() {
        return repository.findAllByOrderByIdDesc();
    }

    /** 查：单条 */
    @GetMapping("/messages/{id}")
    public ResponseEntity<Message> get(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 增 */
    @PostMapping("/messages")
    public ResponseEntity<Message> create(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(repository.save(new Message(content)));
    }

    /** 改 */
    @PutMapping("/messages/{id}")
    public ResponseEntity<Message> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return repository.findById(id)
                .map(m -> {
                    m.setContent(content);
                    return ResponseEntity.ok(repository.save(m));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** 删 */
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** 健康检查：返回处理请求的 Pod 名与记录数 */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "UP");
        m.put("pod", hostname);
        m.put("count", repository.count());
        return m;
    }
}
