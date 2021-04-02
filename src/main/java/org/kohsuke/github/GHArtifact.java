package org.kohsuke.github;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.function.InputStreamFunction;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * An artifact from a workflow run.
 *
 * @author Guillaume Smet
 */
public class GHArtifact extends GHObject {

    // Not provided by the API.
    @JsonIgnore
    private GHRepository owner;

    private String name;
    private long sizeInBytes;
    private String archiveDownloadUrl;
    private boolean expired;
    private String expiresAt;

    public String getName() {
        return name;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public URL getArchiveDownloadUrl() {
        return GitHubClient.parseURL(archiveDownloadUrl);
    }

    public boolean isExpired() {
        return expired;
    }

    public Date getExpiresAt() {
        return GitHubClient.parseDate(expiresAt);
    }

    /**
     * @deprecated This object has no HTML URL.
     */
    @Override
    public URL getHtmlUrl() throws IOException {
        return null;
    }

    /**
     * Deletes the artifact.
     *
     * @throws IOException
     *             the io exception
     */
    public void delete() throws IOException {
        root.createRequest().method("DELETE").withUrlPath(getApiRoute()).fetchHttpStatusCode();
    }

    /**
     * Downloads the artifact.
     *
     * @param <T>
     *            the type of result
     * @param streamFunction
     *            The {@link InputStreamFunction} that will process the stream
     * @throws IOException
     *             The IO exception.
     * @return the result of reading the stream.
     */
    public <T> T download(InputStreamFunction<T> streamFunction) throws IOException {
        requireNonNull(streamFunction, "Stream function must not be null");

        return root.createRequest().method("GET").withUrlPath(getApiRoute(), "zip").fetchStream(streamFunction);
    }

    private String getApiRoute() {
        if (owner == null) {
            // Workflow runs returned from search to do not have an owner. Attempt to use url.
            final URL url = Objects.requireNonNull(getUrl(), "Missing instance URL!");
            return StringUtils.prependIfMissing(url.toString().replace(root.getApiUrl(), ""), "/");
        }
        return "/repos/" + owner.getOwnerName() + "/" + owner.getName() + "/actions/artifacts/" + getId();
    }

    GHArtifact wrapUp(GHRepository owner) {
        this.owner = owner;
        return wrapUp(owner.root);
    }

    GHArtifact wrapUp(GitHub root) {
        this.root = root;
        if (owner != null)
            owner.wrap(root);
        return this;
    }
}