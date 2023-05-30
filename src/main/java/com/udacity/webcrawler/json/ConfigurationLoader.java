package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonParser;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;


/**
 * A static utility class that loads a JSON configuration file.
 *
 */
public final class ConfigurationLoader {

  private final Path path;

  /**
   * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
   */
  public ConfigurationLoader(Path path) {
    this.path = Objects.requireNonNull(path);
  }

  /**
   * Loads configuration from this {@link ConfigurationLoader}'s path
   *
   * @return the loaded {@link CrawlerConfiguration}.
   */

  public CrawlerConfiguration load() {
    //The load() method should open the file at the given path and pass it to the read(Reader reader) method.
    //The load() method should return the CrawlerConfiguration that is returnedby the read(Reader reader) method.
    try (Reader reader = Files.newBufferedReader(path.toAbsolutePath(), StandardCharsets.UTF_8)) {
      return read(reader);
    } catch (IOException e) {
      e.printStackTrace();   
      return new CrawlerConfiguration.Builder().build();
    }
    
  }



    /**
     * Loads crawler configuration from the given reader.
     *
     * @param reader a Reader pointing to a JSON string that contains crawler configuration.
     * @return a crawler configuration
     */

    public static CrawlerConfiguration read(Reader reader){
      Objects.requireNonNull(reader);
      ObjectMapper mapper = new ObjectMapper();
      //create json parser with json factory
      mapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
      try {
        return mapper.readValue(reader, CrawlerConfiguration.class);
      } catch (IOException e) {
        e.printStackTrace();      
        return new CrawlerConfiguration.Builder().build();
      }
    }
  
}



