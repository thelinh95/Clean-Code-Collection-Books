package com.example.orderimport.importdata;

import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;

/**
 * Persist a chunk, push SQL with flush(), then drop the persistence context.
 * flush() does not free memory; clear() does.
 */
@Component
public class ChunkPersister {

    /** Must match spring.jpa.properties.hibernate.jdbc.batch_size */
    public static final int JDBC_BATCH_SIZE = 50;

    /** Persistence-context cap: 10 JDBC batches per flush/clear. */
    public static final int CHUNK_SIZE = JDBC_BATCH_SIZE * 10;

    private final EntityManager entityManager;

    public ChunkPersister(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public <T> void persistChunk(List<T> chunk) {
        if (chunk.isEmpty()) {
            return;
        }
        for (T entity : chunk) {
            entityManager.persist(entity);
        }
        entityManager.flush();
        entityManager.clear();
        chunk.clear();
    }
}
