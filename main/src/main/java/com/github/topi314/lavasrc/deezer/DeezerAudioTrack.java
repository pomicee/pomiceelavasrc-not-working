package com.github.topi314.lavasrc.deezer;

import com.github.topi314.lavasrc.ExtendedAudioTrack;
import com.github.topi314.lavasrc.LavaSrcTools;
import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;

public class DeezerAudioTrack extends ExtendedAudioTrack {

	private final DeezerAudioSourceManager sourceManager;

	public DeezerAudioTrack(AudioTrackInfo trackInfo, DeezerAudioSourceManager sourceManager) {
		this(trackInfo, null, null, null, null, null, false, sourceManager);
	}

	public DeezerAudioTrack(AudioTrackInfo trackInfo, String albumName, String albumUrl, String artistUrl, String artistArtworkUrl, String previewUrl, boolean isPreview, DeezerAudioSourceManager sourceManager) {
		super(trackInfo, albumName, albumUrl, artistUrl, artistArtworkUrl, previewUrl, isPreview);
		this.sourceManager = sourceManager;
	}

	private URI getTrackMediaURI() throws IOException, URISyntaxException {
		var getSessionID = new HttpPost(DeezerAudioSourceManager.PRIVATE_API_BASE + "?method=deezer.ping&input=3&api_version=1.0&api_token=");
		var json = LavaSrcTools.fetchResponseAsJson(this.sourceManager.getHttpInterface(), getSessionID);

		this.checkResponse(json, "Failed to get session ID: ");
		var sessionID = json.get("results").get("SESSION").text();

		var getUserToken = new HttpPost(DeezerAudioSourceManager.PRIVATE_API_BASE + "?method=deezer.getUserData&input=3&api_version=1.0&api_token=");
		getUserToken.setHeader("Cookie", "sid=" + sessionID);
		json = LavaSrcTools.fetchResponseAsJson(this.sourceManager.getHttpInterface(), getUserToken);

		this.checkResponse(json, "Failed to get user token: ");
		var userLicenseToken = json.get("results").get("USER").get("OPTIONS").get("license_token").text();
		var apiToken = json.get("results").get("checkForm").text();

		var getTrackToken = new HttpPost(DeezerAudioSourceManager.PRIVATE_API_BASE + "?method=song.getData&input=3&api_version=1.0&api_token=" + apiToken);
		getTrackToken.setEntity(new StringEntity("{\"sng_id\":\"" + this.trackInfo.identifier + "\"}", ContentType.APPLICATION_JSON));
		json = LavaSrcTools.fetchResponseAsJson(this.sourceManager.getHttpInterface(), getTrackToken);

		this.checkResponse(json, "Failed to get track token: ");
		var trackToken = json.get("results").get("TRACK_TOKEN").text();

		var getMediaURL = new HttpPost(DeezerAudioSourceManager.MEDIA_BASE + "/get_url");
		getMediaURL.setEntity(new StringEntity("{\"license_token\":\"" + userLicenseToken + "\",\"media\": [{\"type\": \"FULL\",\"formats\": [{\"cipher\": \"BF_CBC_STRIPE\", \"format\": \"MP3_128\"}]}],\"track_tokens\": [\"" + trackToken + "\"]}", ContentType.APPLICATION_JSON));
		json = LavaSrcTools.fetchResponseAsJson(this.sourceManager.getHttpInterface(), getMediaURL);

		this.checkResponse(json, "Failed to get media URL: ");
		return new URI(json.get("data").index(0).get("media").index(0).get("sources").index(0).get("url").text());
	}

	private void checkResponse(JsonBrowser json, String message) throws IllegalStateException {
		if (json == null) {
			throw new IllegalStateException(message + "No response");
		}
		var errors = json.get("data").index(0).get("errors").values();
		if (!errors.isEmpty()) {
			var errorsStr = errors.stream().map(error -> error.get("code").text() + ": " + error.get("message").text()).collect(Collectors.joining(", "));
			throw new IllegalStateException(message + errorsStr);
		}
	}

	private byte[] getTrackDecryptionKey() throws NoSuchAlgorithmException {
		var md5 = Hex.encodeHex(MessageDigest.getInstance("MD5").digest(this.trackInfo.identifier.getBytes()), true);
		var master_key = this.sourceManager.getMasterDecryptionKey().getBytes();

		var key = new byte[16];
		for (int i = 0; i < 16; i++) {
			key[i] = (byte) (md5[i] ^ md5[i + 16] ^ master_key[i]);
		}
		return key;
	}

	@Override
	public void process(LocalAudioTrackExecutor executor) throws Exception {
		try (var httpInterface = this.sourceManager.getHttpInterface()) {
			if (this.isPreview) {
				if (this.previewUrl == null) {
					throw new FriendlyException("No preview url found", FriendlyException.Severity.COMMON, new IllegalArgumentException());
				}
				try (var stream = new PersistentHttpStream(httpInterface, new URI(this.previewUrl), this.trackInfo.length)) {
					processDelegate(new Mp3AudioTrack(this.trackInfo, stream), executor);
				}
			} else {
				try (var stream = new DeezerPersistentHttpStream(httpInterface, this.getTrackMediaURI(), this.trackInfo.length, this.getTrackDecryptionKey())) {
					processDelegate(new Mp3AudioTrack(this.trackInfo, stream), executor);
				}
			}
		}
	}

	@Override
	protected AudioTrack makeShallowClone() {
		return new DeezerAudioTrack(this.trackInfo, this.albumName, this.albumUrl, this.artistUrl, this.artistArtworkUrl, this.previewUrl, this.isPreview, this.sourceManager);
	}

	@Override
	public AudioSourceManager getSourceManager() {
		return this.sourceManager;
	}

}
