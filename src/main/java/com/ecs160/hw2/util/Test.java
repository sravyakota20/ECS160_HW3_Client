package com.ecs160.hw2.util;

import com.google.gson.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class JsonParserUtility {

    /**
     * Helper inner class to hold a top-level post along with its original thread JSON.
     */
    private static class PostWithThread {
        Post post;
        JsonObject threadJson;

        PostWithThread(Post post, JsonObject threadJson) {
            this.post = post;
            this.threadJson = threadJson;
        }
    }

    /**
     * Parses the input.json file and returns a List of the top 10 most-liked top-level posts,
     * each with its immediate replies (ignoring further nested replies).
     * For top-level posts, the likeCount is read, but for replies we only keep the content.
     */
    public static List<Post> getTop10TopLevelPosts() {
        List<PostWithThread> allPosts = new ArrayList<>();
        try (InputStream in = JsonParserUtility.class.getClassLoader().getResourceAsStream("input.json")) {
            if (in == null) {
                System.err.println("input.json not found in resources.");
                return new ArrayList<>();
            }
            JsonElement element = JsonParser.parseReader(new InputStreamReader(in));
            if (element.isJsonObject()) {
                JsonObject jsonObject = element.getAsJsonObject();
                JsonArray feedArray = jsonObject.getAsJsonArray("feed");
                for (JsonElement feedElement : feedArray) {
                    JsonObject feedObj = feedElement.getAsJsonObject();
                    if (feedObj.has("thread")) {
                        JsonObject threadObj = feedObj.getAsJsonObject("thread");
                        if (threadObj.has("post")) {
                            JsonObject postObj = threadObj.getAsJsonObject("post");
                            Post topPost = parsePost(postObj);
                            if (postObj.has("likeCount")) {
                                topPost.setLikeCount(postObj.get("likeCount").getAsInt());
                            }
                            // Save the top-level post along with its entire thread JSON (for later reply parsing)
                            allPosts.add(new PostWithThread(topPost, threadObj));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Sort by likeCount descending
        allPosts.sort((p1, p2) -> Integer.compare(p2.post.getLikeCount(), p1.post.getLikeCount()));
        // Select top 10
        int limit = Math.min(10, allPosts.size());
        List<Post> top10Posts = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            PostWithThread pwt = allPosts.get(i);
            // If there are immediate replies, parse them (only content is needed)
            if (pwt.threadJson.has("replies")) {
                JsonElement repliesElem = pwt.threadJson.get("replies");
                if (repliesElem.isJsonArray()) {
                    JsonArray repliesArray = repliesElem.getAsJsonArray();
                    List<Post> replies = parseRepliesContent(repliesArray);
                    pwt.post.setReplies(replies);
                }
            }
            top10Posts.add(pwt.post);
        }
        return top10Posts;
    }

    /**
     * Parses immediate replies from a replies JSON array.
     * For each reply, only the content and createdAt are extracted.
     * Nested replies are ignored.
     */
    private static List<Post> parseRepliesContent(JsonArray repliesArray) {
        List<Post> replies = new ArrayList<>();
        for (JsonElement replyElement : repliesArray) {
            if (replyElement.isJsonObject()) {
                JsonObject replyObj = replyElement.getAsJsonObject();
                if (replyObj.has("post")) {
                    JsonObject replyPostObj = replyObj.getAsJsonObject("post");
                    // Use a simplified parse that only reads content and createdAt.
                    Post replyPost = parseReplyContent(replyPostObj);
                    replies.add(replyPost);
                }
            }
        }
        return replies;
    }

    /**
     * Parses a top-level post from a JSON object.
     * This method extracts the URI (to derive a postId), text, and createdAt.
     */
    private static Post parsePost(JsonObject postObj) {
        String uri = postObj.has("uri") ? postObj.get("uri").getAsString() : "";
        int postId = extractPostIdFromUri(uri);
        String text = "";
        String createdAt = "";
        if (postObj.has("record")) {
            JsonObject recordObj = postObj.getAsJsonObject("record");
            text = recordObj.has("text") ? recordObj.get("text").getAsString() : "";
            createdAt = recordObj.has("createdAt") ? recordObj.get("createdAt").getAsString() : "";
        }
        return new Post(postId, text, createdAt);
    }

    /**
     * Parses a reply from a JSON object, but only extracts the content and createdAt.
     * This method ignores likeCount and any other fields that are not needed.
     */
    private static Post parseReplyContent(JsonObject replyPostObj) {
        // We only need to extract the text and createdAt.
        // We still generate a postId from the URI for identification.
        String uri = replyPostObj.has("uri") ? replyPostObj.get("uri").getAsString() : "";
        int postId = extractPostIdFromUri(uri);
        String text = "";
        String createdAt = "";
        if (replyPostObj.has("record")) {
            JsonObject recordObj = replyPostObj.getAsJsonObject("record");
            text = recordObj.has("text") ? recordObj.get("text").getAsString() : "";
            createdAt = recordObj.has("createdAt") ? recordObj.get("createdAt").getAsString() : "";
        }
        // For replies, we don't care about likeCount, so we can set it to 0.
        Post reply = new Post(postId, text, createdAt);
        reply.setLikeCount(0);
        return reply;
    }

    /**
     * Extracts a postId from a given URI string.
     * For example, it uses the hashCode of the last segment of the URI.
     */
    private static int extractPostIdFromUri(String uri) {
        if (uri == null || uri.isEmpty()) return -1;
        String[] parts = uri.split("/");
        return parts.length > 0 ? parts[parts.length - 1].hashCode() : -1;
    }
}
