package com.ecs160.hw2.client;

import com.ecs160.hw2.util.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class PipelineClientTest {

	private PipelineClientHW2 pipelineClient;
	private RestTemplate restTemplate;
	private MockRestServiceServer mockServer;
	private final String moderationServiceUrl = "http://localhost:30000/moderate";

	@BeforeEach
	public void setUp() {
		restTemplate = new RestTemplate();
		// Instantiate the pipeline client with the RestTemplate and moderation service URL.
		pipelineClient = new PipelineClientHW2(restTemplate, moderationServiceUrl);
		// Create a mock server to simulate responses from the moderation service.
		mockServer = MockRestServiceServer.createServer(restTemplate);
	}

	@Test
	public void testProcessPosts() {
		// Create a sample top-level post (with likeCount for top-level posts)
		Post post1 = new Post(101, "This is a safe post", "2024-12-10T00:00:00Z");
		post1.setLikeCount(10);

		// Create an immediate reply for post1 (for replies we only need content)
		Post reply1 = new Post(201, "This is a reply", "2024-12-10T01:00:00Z");
		// For replies, likeCount is not used.
		List<Post> replies1 = new ArrayList<>();
		replies1.add(reply1);
		post1.setReplies(replies1);

		// Create another top-level post without replies.
		Post post2 = new Post(102, "This is another safe post", "2024-12-10T02:00:00Z");
		post2.setLikeCount(8);

		// Prepare the list of posts.
		List<Post> posts = new ArrayList<>();
		posts.add(post1);
		posts.add(post2);

		// Set up the expected moderation responses.
		// For post1:
		mockServer.expect(requestTo(moderationServiceUrl))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess("This is a safe post #tag1", MediaType.APPLICATION_JSON));
		// For reply1:
		mockServer.expect(requestTo(moderationServiceUrl))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess("This is a reply #tagReply", MediaType.APPLICATION_JSON));
		// For post2:
		mockServer.expect(requestTo(moderationServiceUrl))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess("This is another safe post #tag2", MediaType.APPLICATION_JSON));

		// Invoke the pipeline client to process the posts.
		List<String> output = pipelineClient.processPosts(posts);

		// Verify the output contains expected strings.
		// Expected output lines:
		// "Post ID: 101"
		// "> This is a safe post #tag1"
		// "  --> This is a reply #tagReply"
		// "Post ID: 102"
		// "> This is another safe post #tag2"
		assertTrue(output.contains("Post ID: 101"));
		assertTrue(output.contains("> This is a safe post #tag1"));
		assertTrue(output.contains("  --> This is a reply #tagReply"));
		assertTrue(output.contains("Post ID: 102"));
		assertTrue(output.contains("> This is another safe post #tag2"));

		// Verify that all expected requests were made.
		mockServer.verify();
	}
}

