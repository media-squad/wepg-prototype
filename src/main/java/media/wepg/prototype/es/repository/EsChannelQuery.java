package media.wepg.prototype.es.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import media.wepg.prototype.es.util.BulkResponseResolver;
import media.wepg.prototype.orig.model.ChannelOrigin;
import media.wepg.prototype.orig.repository.ChannelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Repository
public class EsChannelQuery {
    private static final Logger logger = LoggerFactory.getLogger(EsChannelQuery.class);


    private final ElasticsearchClient esClient;
    private final ChannelRepository channelRepository;


    private static final String CHANNEL_INDEX_NAME = "prototype-channels";

    @Autowired
    public EsChannelQuery(ElasticsearchClient esClient, ChannelRepository channelRepository) {
        this.esClient = esClient;
        this.channelRepository = channelRepository;
    }


    public Optional<media.wepg.prototype.es.model.Channel> getDocumentById(Long id) throws IOException {
        GetResponse<media.wepg.prototype.es.model.Channel> response = esClient.get(g -> g
                        .index(CHANNEL_INDEX_NAME)
                        .id(id.toString()),
                media.wepg.prototype.es.model.Channel.class
        );

        if (response.found()) {
            media.wepg.prototype.es.model.Channel channel = response.source();
            logger.info("Channel name" + channel.getChannelName());
            return Optional.of(channel);
        }

        logger.info("Channel not found!");
        return Optional.empty();
    }

    public void fetchAndIndexChannelData() throws IOException {
        List<ChannelOrigin> channelOriginData = channelRepository.findAll();

        createNewIndexIfNotExists(CHANNEL_INDEX_NAME);

        indexChannelData(channelOriginData);
    }

    public boolean validateIndexExisting(String indexName) throws IOException {
        return esClient.indices().exists(i -> i.index(indexName)).value();
    }

    private void createNewIndexIfNotExists(String indexName) throws IOException {
        if (!validateIndexExisting(indexName)) {
            esClient.indices().create(i -> i.index(indexName)
            );
        }
    }


    private void indexChannelData(List<ChannelOrigin> channels) throws IOException {
        channels.forEach(this::bulkChannelIndex);
    }

    private void bulkChannelIndex(ChannelOrigin channel) throws RuntimeException {
        BulkResponse bulkResponse = null;
        try {
            bulkResponse = getChannelDataBulkResponse(channel);
            BulkResponseResolver.resolveBulkResponse(bulkResponse, logger);
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private BulkResponse getChannelDataBulkResponse(ChannelOrigin ch) throws IOException {
        return esClient.bulk(new BulkRequest.Builder().operations(op ->
                op.index(idx -> idx
                        .index(CHANNEL_INDEX_NAME)
                        .id(ch.getId().toString())
                        .document(ch)
                )
        ).build());
    }
}