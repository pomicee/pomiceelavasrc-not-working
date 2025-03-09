package com.github.topi314.lavasrc.plugin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "plugins.lavasrc.sources")
@Component
public class SourcesConfig {

  private boolean spotify = false;
  private boolean appleMusic = false;
  private boolean deezer = false;
  private boolean yandexMusic = false;
  private boolean floweryTTS = false;
  private boolean youtube = false;
  private boolean sliderkz = false;
  private boolean tidal = false;
  private boolean pandora = false;



  public boolean isSpotify() {
    return this.spotify;
  }

  public void setSpotify(boolean spotify) {
    this.spotify = spotify;
  }
  public boolean isTidal() {
    return this.tidal;
  }

  public void setTidal(boolean tidal) {
    this.tidal = tidal;
  }
    public boolean isPandora() {
    return this.pandora;
  }

  public void setPandora(boolean pandora) {
    this.pandora = pandora;
  }

  public boolean isAppleMusic() {
    return this.appleMusic;
  }

  public void setAppleMusic(boolean appleMusic) {
    this.appleMusic = appleMusic;
  }

  public boolean isDeezer() {
    return this.deezer;
  }

  public void setDeezer(boolean deezer) {
    this.deezer = deezer;
  }

  public boolean isYandexMusic() {
    return this.yandexMusic;
  }

  public void setYandexMusic(boolean yandexMusic) {
    this.yandexMusic = yandexMusic;
  }

  public boolean isFloweryTTS() {
    return this.floweryTTS;
  }

  public void setFloweryTTS(boolean floweryTTS) {
    this.floweryTTS = floweryTTS;
  }

  public boolean isYoutube() {
    return this.youtube;
  }

  public void setYoutube(boolean youtube) {
    this.youtube = youtube;
  }
  
  public boolean isSliderkz() {
        return this.sliderkz;
    }

    public void setSliderkz(boolean sliderkz) {
        this.sliderkz = sliderkz;
    }
}
