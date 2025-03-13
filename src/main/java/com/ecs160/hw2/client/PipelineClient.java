package com.ecs160.hw2.client;

import com.ecs160.hw2.util.JsonParserUtility;
import com.ecs160.hw2.util.Post;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class PipelineClient {
    public static void main(String[] args) {
        // get the top 10 most liked posts
        List<Post> topPosts = JsonParserUtility.getTop10TopLevelPosts();
        System.out.println("Top 10 posts");
        for (Post post: topPosts ) {
            System.out.println(post);
        }

        // run thru each post and send to the Moderator microservice
        RestTemplate restTemplate = new RestTemplate();
        String port = System.getProperties().getProperty("server.port", "30000");
        String moderationServiceUrl =  "http://localhost:" + port + "/moderate";

        for (Post post : topPosts) {
            String moderatedContent = sendModerationRequest(restTemplate, moderationServiceUrl, post.getPostContent());
            System.out.println("Post ID: " + post.getPostId());
            System.out.println("> " + moderatedContent);

            // Process each reply similarly
            if (post.getReplies() != null && !post.getReplies().isEmpty()) {
                for (Post reply : post.getReplies()) {
                    //System.out.println("Sending Reply post for Moderation");
                    String moderatedReply = sendModerationRequest(restTemplate, moderationServiceUrl, reply.getPostContent());
                    System.out.println("Post ID: " + post.getPostId());
                    System.out.println("> " + moderatedReply);
                }
            }
        }
    }

    private static String sendModerationRequest(RestTemplate restTemplate, String url, String content) {
        ModerationRequest req = new ModerationRequest();
        req.setPostContent(content);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ModerationRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return response.getBody();
    }

    // Request payload class
    static class ModerationRequest {
        private String postContent;
        public String getPostContent() { return postContent; }
        public void setPostContent(String postContent) { this.postContent = postContent; }
    }
}
