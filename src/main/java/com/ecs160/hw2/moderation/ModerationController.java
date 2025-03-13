package com.ecs160.hw2.moderation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@RestController
public class ModerationController {

    // Banned words list
    private final List<String> bannedWords = Arrays.asList(
            "illegal", "fraud", "scam", "exploit", "dox", "swatting", "hack", "crypto", "bots"
    );

    private final RestTemplate restTemplate = new RestTemplate();
    // URL for the Hashtagging service – make sure this matches the port of your hashtagging microservice.
    private final String hashtagServiceUrl = "http://localhost:30001/hashtag";

    @PostMapping("/moderate")
    public ResponseEntity<String> moderate(@RequestBody PostRequest request) {
        String content = request.getPostContent();
        // Moderation check: if any banned word exists, return [DELETED]
        for (String banned : bannedWords) {
            if (content.toLowerCase().contains(banned)) {
                return ResponseEntity.ok("[DELETED]");
            }
        }
        // Moderation passed. Call the hashtagging microservice.
        try {
            ResponseEntity<HashtagResponse> response = restTemplate.postForEntity(
                    hashtagServiceUrl, request, HashtagResponse.class
            );
            if (response.getBody() != null && response.getBody().getHashtag() != null) {
                // Append the generated hashtag to the post content.
                return ResponseEntity.ok(content + " " + response.getBody().getHashtag());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // In case of error, use default hashtag.
            return ResponseEntity.ok(content + " " + "#bskypost");
        }
        // Fallback
        return ResponseEntity.ok(content + " " + "#bskypost");
    }

    // Request payload
    static class PostRequest {
        private String postContent;

        public String getPostContent() {
            return postContent;
        }
        public void setPostContent(String postContent) {
            this.postContent = postContent;
        }
    }

    // Response payload from Hashtagging service
    static class HashtagResponse {
        private String hashtag;

        public String getHashtag() {
            return hashtag;
        }
        public void setHashtag(String hashtag) {
            this.hashtag = hashtag;
        }
    }
}
