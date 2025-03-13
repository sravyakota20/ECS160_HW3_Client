package com.ecs160.hw2.client;

import com.ecs160.hw2.util.Post;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;

public class PipelineClientHW2 {

    private final RestTemplate restTemplate;
    private final String moderationServiceUrl;

    public PipelineClientHW2(RestTemplate restTemplate, String moderationServiceUrl) {
        this.restTemplate = restTemplate;
        this.moderationServiceUrl = moderationServiceUrl;
    }

    /**
     * Processes a list of posts by sending each post (and its immediate replies)
     * to the moderation service. Returns a list of output strings.
     */
    public List<String> processPosts(List<Post> posts) {
        List<String> output = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (Post post : posts) {
            ModerationRequest request = new ModerationRequest();
            request.setPostContent(post.getPostContent());
            HttpEntity<ModerationRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(moderationServiceUrl, entity, String.class);
            String moderatedContent = response.getBody();
            output.add("Post ID: " + post.getPostId());
            output.add("> " + moderatedContent);

            if (post.getReplies() != null && !post.getReplies().isEmpty()) {
                for (Post reply : post.getReplies()) {
                    ModerationRequest replyRequest = new ModerationRequest();
                    replyRequest.setPostContent(reply.getPostContent());
                    HttpEntity<ModerationRequest> replyEntity = new HttpEntity<>(replyRequest, headers);
                    ResponseEntity<String> replyResponse = restTemplate.postForEntity(moderationServiceUrl, replyEntity, String.class);
                    String moderatedReply = replyResponse.getBody();
                    output.add("  --> " + moderatedReply);
                }
            }
        }
        return output;
    }

    public static class ModerationRequest {
        private String postContent;

        public String getPostContent() {
            return postContent;
        }

        public void setPostContent(String postContent) {
            this.postContent = postContent;
        }
    }
}