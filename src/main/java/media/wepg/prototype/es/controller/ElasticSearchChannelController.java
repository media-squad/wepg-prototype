package media.wepg.prototype.es.controller;

import lombok.RequiredArgsConstructor;
import media.wepg.prototype.es.model.Channel;
import media.wepg.prototype.es.repository.ElasticSearchBatchQuery;
import media.wepg.prototype.es.repository.ElasticSearchChannelQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wepg/channel")
public class ElasticSearchChannelController {

    private ElasticSearchChannelQuery elasticSearchChannelQuery;
    private ElasticSearchBatchQuery elasticSearchBatchQuery;

    @Autowired
    public ElasticSearchChannelController(ElasticSearchChannelQuery elasticSearchChannelQuery, ElasticSearchBatchQuery elasticSearchBatchQuery) {
        this.elasticSearchChannelQuery = elasticSearchChannelQuery;
        this.elasticSearchBatchQuery = elasticSearchBatchQuery;
    }


    @PostMapping("/createOrUpdateDocument")
    public ResponseEntity<Object> createOrUpdateDocument(@RequestBody Channel channel) throws IOException {
        String result = elasticSearchChannelQuery.createOrUpdateDocument(channel);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/getDocument/{id}")
    public ResponseEntity<Channel> getDocumentById(@PathVariable("id") Long id) throws IOException {
        Optional<Channel> documentById = elasticSearchChannelQuery.getDocumentById(id);

        return documentById
                .map(channel -> ResponseEntity.ok().body(channel))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/updateAllPrograms")
    public ResponseEntity updateAllPrograms() throws IOException {
        elasticSearchBatchQuery.fetchAndIndexProgramData();

        return ResponseEntity.ok()
                .body("All programs were updated");
    }

}
