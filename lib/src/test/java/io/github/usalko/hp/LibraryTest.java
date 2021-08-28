/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package io.github.usalko.hp;

import static java.text.MessageFormat.format;
import static org.jcodec.common.io.NIOUtils.writableChannel;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.usalko.hp.parser.IPlaylist;
import io.github.usalko.hp.parser.MasterPlaylist;
import io.github.usalko.hp.parser.MediaPlaylist;
import io.github.usalko.hp.parser.PlaylistFactory;
import io.github.usalko.hp.parser.PlaylistVersion;
import io.github.usalko.hp.parser.tags.master.StreamInf;
import io.github.usalko.hp.parser.tags.media.ExtInf;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mps.MPEGDemuxer.MPEGDemuxerTrack;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.netty.http.client.HttpClient;

class LibraryTest {

    @Test
    void someLibraryMethodReturnsTrue() {
        Library classUnderTest = new Library();
        assertTrue(classUnderTest.someLibraryMethod(), "someLibraryMethod should return 'true'");
    }

    @Test
    void simpleHttpRequest() {
        String response = HttpClient.create()           // Prepares an HTTP client ready for configuration
                .host("google.com")
                .port(80)  // Obtains the server's port and provides it as a port to which this
                // client should connect
                .get()               // Specifies that POST method will be used
                .uri("/")   // Specifies the path
                .responseContent()    // Receives the response body
                .aggregate()
                .asString()
                .log("http-client")
                .block();
        assertNotNull(response);
    }

    @Test
    void parserMasterListTest() throws IOException {
        List<String> messages = new ArrayList<>();

        URL masterListUrl = new URL(
                "https://cdn.zefshar.com/videos/31836_pWjXWuGkVG3utgRQ/master.m3u8");
        IPlaylist playlist = PlaylistFactory.parsePlaylist(PlaylistVersion.TWELVE,
                masterListUrl,
                (int) Duration.of(2, ChronoUnit.SECONDS).toMillis());

        URL topBitrateVariant = null;
        if (playlist.isMasterPlaylist()) {

            MasterPlaylist mp = (MasterPlaylist) playlist;
            for (StreamInf stream : mp.getVariantStreams()) {
                messages.add(
                        format(
                                "Program ID: [{0}]; Bandwidth: [{1}]; Codecs: [{2}]; Resolution: [{3}]; URI: [{4}];",
                                stream.getProgramId(),
                                String.valueOf(stream.getBandwidth()),
                                stream.getCodecs(),
                                stream.getResolution(),
                                stream.getURI()));
            }

            int highestBitrate = Integer.MIN_VALUE;
            for (StreamInf variant : ((MasterPlaylist) playlist).getVariantStreams()) {
                if (variant.getBandwidth() > highestBitrate) {
                    topBitrateVariant = U.from(masterListUrl)
                            .replacePathSegment(-1, variant.getURI()).toUrl();
                }
            }
        }
        System.out.println(format("Messages are: {0}", messages));
        assertFalse(messages.isEmpty());
        System.out.println(format("Top bitrate variant is: {0}", topBitrateVariant));
        assertNotNull(topBitrateVariant);
    }

    @Test
    void parserTsListTest() throws IOException {
        List<String> messages = new ArrayList<>();

        URL masterListUrl = new URL(
                "https://cdn.zefshar.com/videos/31836_pWjXWuGkVG3utgRQ/index-svod360n-v1-a1.m3u8");
        IPlaylist playlist = PlaylistFactory.parsePlaylist(PlaylistVersion.TWELVE,
                masterListUrl,
                (int) Duration.of(2, ChronoUnit.SECONDS).toMillis());
        assertTrue(playlist instanceof MediaPlaylist);
        System.out.println(
                format("Media play list for {0}: {1}", masterListUrl, playlist));
    }

    @Test
    @Disabled("Manual run only")
    void downloadSegment() throws IOException {
        List<String> messages = new ArrayList<>();

        URL masterListUrl = new URL(
                "https://cdn.zefshar.com/videos/31836_pWjXWuGkVG3utgRQ/index-svod360n-v1-a1.m3u8");
        IPlaylist playlist = PlaylistFactory.parsePlaylist(PlaylistVersion.TWELVE,
                masterListUrl,
                (int) Duration.of(2, ChronoUnit.SECONDS).toMillis());
        System.out.println(format("Media play list for {0}: {1}", masterListUrl, playlist));
        assertTrue(playlist instanceof MediaPlaylist);
        MediaPlaylist mediaPlayList = ((MediaPlaylist) playlist);

        for (ExtInf segment : mediaPlayList.getSegments()) {
            URL downloadRef = U.from(masterListUrl).replacePathSegment(-1, segment.getURI())
                    .toUrl();
            assertNotNull(downloadRef);
            // Download refs to the file
            try (RandomAccessFile writer = new RandomAccessFile(segment.getURI(), "rw");
                    FileChannel channel = writer.getChannel()) {
                HttpClient.create()
                        .compress(true)
                        .secure()
                        .get()
                        .uri(downloadRef.toString())
                        .responseContent()
                        .asByteBuffer()
                        .map((byteBuffer) -> {
                            try {
                                return channel.write(byteBuffer);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }).blockLast();
            }

            // Read the file and remux to the mp4
            String fileName = segment.getURI();
            FileInputStream demInputStream = new FileInputStream(fileName);
            MTSDemuxer demuxer = new MTSDemuxer(
                    new FileChannelWrapper(demInputStream.getChannel()));
            System.out.println(format("Programs {0}", demuxer.getPrograms()));
            String outputFileName = fileName + ".mp4";
            SeekableByteChannel file = writableChannel(new File(outputFileName));
            MP4Muxer muxer = MP4Muxer.createMP4MuxerToChannel(file);

            for (Integer programIndex : demuxer.getPrograms()) {
                ReadableByteChannel channel = demuxer.getProgram(programIndex);
                MPSDemuxer mpsDemuxer = new MPSDemuxer(channel);
                for (MPEGDemuxerTrack audioTrack : mpsDemuxer.getAudioTracks()) {
                    System.out.println(audioTrack);
                    AudioCodecMeta audioCodecMeta =
                            audioTrack.getMeta() != null ? audioTrack.getMeta().getAudioCodecMeta()
                                    : AudioCodecMeta.fromAudioFormat(AudioFormat.NCH_48K_S24_LE(2));
                    MuxerTrack outputAudioTrack = muxer.addAudioTrack(Codec.AAC, audioCodecMeta);
                    Packet packet = audioTrack.nextFrameWithBuffer(null);
                    while (packet != null) {
                        if (packet.getDuration() > 0) {
                            outputAudioTrack.addFrame(packet);
                        }
                        packet = audioTrack.nextFrameWithBuffer(null);
                    }
                }
                for (MPEGDemuxerTrack videoTrack : mpsDemuxer.getVideoTracks()) {
                    System.out.println(videoTrack);
                    VideoCodecMeta videoCodecMeta =
                            videoTrack.getMeta() != null ? videoTrack.getMeta().getVideoCodecMeta()
                                    : VideoCodecMeta.createSimpleVideoCodecMeta(new Size(640, 368),
                                            ColorSpace.YUV420);
                    MuxerTrack outputVideoTrack = muxer.addVideoTrack(Codec.H264, videoCodecMeta);
                    Packet packet = videoTrack.nextFrameWithBuffer(null);
                    while (packet != null) {
                        outputVideoTrack.addFrame(packet);
                        packet = videoTrack.nextFrameWithBuffer(null);
                    }
                }
            }

            muxer.finish();

            file.close();

            System.out.println(format("Link {0} saved and processed to file {1}", downloadRef,
                    outputFileName));
        }
    }

}
