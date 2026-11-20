package com.janmanch.processing;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/** Kafka topic used between the upload API and the processing consumer. */
@Configuration
public class VideoTopics {

    /** The message key and value are both the video id. */
    public static final String UPLOADED = "janmanch.video-uploaded";

    @Bean
    public NewTopic videoUploadedTopic() {
        return TopicBuilder.name(UPLOADED).partitions(3).replicas(1).build();
    }
}
