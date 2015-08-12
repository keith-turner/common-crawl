package io.fluo.commoncrawl.data;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import io.fluo.api.client.LoaderExecutor;
import io.fluo.api.config.ObserverConfiguration;
import io.fluo.commoncrawl.data.observers.AddLinkObserver;
import io.fluo.commoncrawl.data.util.Page;
import io.fluo.core.client.LoaderExecutorImpl;
import io.fluo.core.impl.Environment;
import io.fluo.integration.ITBaseMini;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadIT extends ITBaseMini {

  private static final Logger log = LoggerFactory.getLogger(LoadIT.class);

  @Override
  protected List<ObserverConfiguration> getObservers() {
    return Collections.singletonList(new ObserverConfiguration(AddLinkObserver.class.getName()));
  }

  @Test
  public void testLoad() throws Exception {

    config.setLoaderThreads(0);
    config.setLoaderQueueSize(0);

    try (Environment env = new Environment(config);
         LoaderExecutor le = new LoaderExecutorImpl(config, env)) {

      ArchiveReader ar = WARCReaderFactory.get(new File("src/test/resources/wat-18.warc"));

      Iterator<ArchiveRecord> records = ar.iterator();
      while (records.hasNext()) {
        try {
          ArchiveRecord r = records.next();
          Page p = Page.from(r);
          if (p.isEmpty() || p.getExternalLinks().isEmpty()) {
            continue;
          }
          log.info("Loading page {} with {} links", p.getLink().getUrl(), p.getExternalLinks().size());
          le.execute(new PageLoader(p));
        } catch (ParseException e) {
          log.debug("Parse exception occurred", e);
        }


      }
      ar.close();

      miniFluo.waitForObservers();
      printSnapshot();
    }
  }
}
