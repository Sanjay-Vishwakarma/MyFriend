package com.realtime.myfriend.config;


import com.realtime.myfriend.entity.CallHistory;
import com.realtime.myfriend.entity.ChatMessage;
import com.realtime.myfriend.entity.User;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import jakarta.annotation.PostConstruct;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfig {

    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;

    public MongoConfig(MongoTemplate mongoTemplate, MongoMappingContext mongoMappingContext) {
        this.mongoTemplate = mongoTemplate;
        this.mongoMappingContext = mongoMappingContext;
    }

    @PostConstruct
    public void initIndexes() {
        createIndexFor(User.class);
        createIndexFor(ChatMessage.class);
        createIndexFor(CallHistory.class);
    }

    private void createIndexFor(Class<?> entityClass) {
        IndexResolver resolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);
        IndexOperations indexOps = mongoTemplate.indexOps(entityClass);
        
        for (IndexDefinition index : resolver.resolveIndexFor(entityClass)) {
            indexOps.ensureIndex(index);
        }
    }
}