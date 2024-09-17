/*
 * Copyright (c) 2024. rharri
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.net.http.HttpRequest.BodyPublishers;
import static java.net.http.HttpResponse.BodyHandlers;

public class Main {

    private final static String DEFAULT_PATH = "./exclude.txt";

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || args.length > 3) {
            System.err.println("Usage: java Main <username> <token> [--dry-run]");
        }

        String username = args[0];
        String token = String.format("Bearer %s", args[1]);
        boolean dryRun = args.length > 2 && args[2].equals("--dry-run");

        // Process excluded repositories (if any)
        Path path = Paths.get(DEFAULT_PATH);
        List<String> lines = new ArrayList<>();
        if (path.toFile().exists()) {
            lines = Files.readAllLines(path);
            lines.removeIf(line -> line.startsWith("#"));
        }

        try (HttpClient client = HttpClient.newHttpClient()) {

            Instant start = Instant.now();

            HttpRequest getPublicRepos = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("https://api.github.com/users/%s/repos", username)))
                    .build();

            HttpResponse<String> publicRepos = client.send(getPublicRepos, BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            List<Repository> repositories =
                    mapper.readerForListOf(Repository.class).readValue(publicRepos.body());

            if (dryRun) {
                System.out.println("Dry run.");
            }

            int privatized = 0;
            for (Repository repository : repositories) {
                if (repository.isFork()) {
                    // Do not privatize forked repositories
                    continue;
                }

                if (lines.contains(repository.fullName())) {
                    // Do not privatize excluded repositories
                    continue;
                }

                Repository privateRepo = new Repository(
                        repository.fullName(), false, true, repository.url());

                if (dryRun) {
                    // Do not actually modify the status of the repository
                    System.out.printf("%s would be set to private%n", repository.fullName());
                    continue;
                }

                HttpRequest setRepoPrivate = HttpRequest.newBuilder()
                        .uri(new URI(repository.url()))
                        .header("Content-Type", "application/json")
                        .header("Authorization", token)
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .method("PATCH", BodyPublishers.ofString(mapper.writeValueAsString(privateRepo)))
                        .build();

                HttpResponse<String> didSetRepoPrivate =
                        client.send(setRepoPrivate, BodyHandlers.ofString());

                if (didSetRepoPrivate.statusCode() == 200) {
                    System.out.printf("%s is now private%n", repository.fullName());
                    privatized += 1;
                }
            }

            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            System.out.printf("Privatized %d repositories in %d ms.%n", privatized, duration.toMillis());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.exit(0);
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record Repository(
        @JsonProperty("full_name") String fullName,
        @JsonProperty("fork") boolean isFork,
        @JsonProperty("private") boolean isPrivate,
        String url) {
}
