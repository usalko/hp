/**
 * Copyright 2015 Comcast Cable Communications Management, LLC
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 * <p>
 * TO-DO: Add tests for: byte range, allow cache, iframes only, discontinuity Create objects for:
 * ext-x-map, ext-x-i-frame-stream-inf, ext-x-start, ext-x-discontinuity-sequence
 * <p>
 * TO-DO: Add tests for: byte range, allow cache, iframes only, discontinuity Create objects for:
 * ext-x-map, ext-x-i-frame-stream-inf, ext-x-start, ext-x-discontinuity-sequence
 * <p>
 * TO-DO: Add tests for: byte range, allow cache, iframes only, discontinuity Create objects for:
 * ext-x-map, ext-x-i-frame-stream-inf, ext-x-start, ext-x-discontinuity-sequence
 */
/**
 * TO-DO:
 * Add tests for: byte range, allow cache, iframes only, discontinuity
 * Create objects for: ext-x-map, ext-x-i-frame-stream-inf, ext-x-start, ext-x-discontinuity-sequence
 */
package io.github.usalko.hp.parser;

import io.github.usalko.hp.parser.v12.MasterPlaylistV12;
import io.github.usalko.hp.parser.v12.MediaPlaylistV12;
import io.netty.channel.ChannelOption;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import reactor.netty.http.client.HttpClient;

/**
 * Provides factory methods to generate and parse playlists.
 */
public class PlaylistFactory {

    static {
        try {
            Class.forName("io.github.usalko.hp.parser.tags.TagNames");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Factory method to generate a playlist object. This method performs no
     * HTTP actions. It uses the playlistStream parameter as the playlist.
     *
     * @param playlistVersion version of the playlist (V12 is the default)
     * @param playlistStream inputStream containing a correctly formatted playlist
     * @return parsed playlist
     * @throws IOException on parsing exception
     */
    public static AbstractPlaylist parsePlaylist(final PlaylistVersion playlistVersion,
            final InputStream playlistStream) throws IOException {
        final PlaylistParser parser = new PlaylistParser();
        parser.parse(playlistStream);
        return getVersionSpecificPlaylist(parser, playlistVersion);
    }

    /**
     * Factory method to generate playlist object.  This method performs no
     * HTTP actions. It uses the playlistString parameter as the playlist.
     *
     * @param playlistVersion version of the playlist (V12 is the default)
     * @param playlistString string containing a correctly formatted playlist
     * @return parsed playlist
     */
    public static AbstractPlaylist parsePlaylist(final PlaylistVersion playlistVersion,
            final String playlistString) {
        final PlaylistParser parser = new PlaylistParser();
        parser.parse(playlistString);
        return getVersionSpecificPlaylist(parser, playlistVersion);
    }

    /**
     * Factory method to generate playlist object. This method uses a very
     * simple HTTP client to download the URL passed by the playlistURL
     * parameters. This method should not be used for applications that require
     * high-performance, as the HTTP connection management is very basic. If
     * your application has high performance requirements us the parsePlaylist
     * method that takes an InputStream.
     *
     * @param playlistVersion version of the playlist (V12 is the default)
     * @param playlistURL URL pointing to a playlist
     * @param connectTimeout timeout (ms) until a connection with the server is established
     * @return parsed playlist
     */
    public static AbstractPlaylist parsePlaylist(final PlaylistVersion playlistVersion,
            final URL playlistURL, final int connectTimeout) {
        HttpClient client = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .compress(true);
        if (playlistURL.getProtocol().equalsIgnoreCase("https")) {
            client = client.secure();
        }

        return client
                .get()
                .uri(playlistURL.toString())
                .responseContent()
                .aggregate()
                .asString()
                .map(response -> {
                    final PlaylistParser parser = new PlaylistParser();
                    parser.parse(response);
                    return getVersionSpecificPlaylist(parser, playlistVersion);
                }).block();
    }

    /**
     * Returns a playlist that represents a specific object version of the playlist.
     * Currently, only V12 is supported.
     * @param parser playlist parser
     * @param playlistVersion preferred playlist version
     * @return playlist
     */
    private static AbstractPlaylist getVersionSpecificPlaylist(final PlaylistParser parser,
            final PlaylistVersion playlistVersion) {
        AbstractPlaylist playlist = null;

        switch (playlistVersion) {
            case TWELVE:
            case DEFAULT:
            default:
                if (parser.isMasterPlaylist()) {
                    playlist = new MasterPlaylistV12(parser.getTags());
                } else {
                    playlist = new MediaPlaylistV12(parser.getTags());
                }

                break;
        }

        return playlist;
    }

    /**
     * Returns a playlist inputStream given an httpClient and a URL.
     * @param httpClient http client
     * @param url URL to a playlist
     * @return inputStream
     * @throws IOException on HTTP connection exception
     */
    //private static InputStream getPlaylistInputStream(final CloseableHttpClient httpClient,
    //        final URL url) throws IOException {
    //    final HttpGet get = new HttpGet(url.toString());
    //    CloseableHttpResponse response = null;
    //    response = httpClient.execute(get);
    //    if (response == null) {
    //        throw new IOException("Request returned a null response");
    //    }
    //    if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
    //        response.close();
    //        throw new IOException("Request returned a status code of "
    //                + response.getStatusLine().getStatusCode());
    //    }
    //
    //    return response.getEntity().getContent();
    //}
}
