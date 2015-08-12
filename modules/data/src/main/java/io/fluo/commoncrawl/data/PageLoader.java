package io.fluo.commoncrawl.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import io.fluo.api.client.Loader;
import io.fluo.api.client.TransactionBase;
import io.fluo.api.config.ScannerConfiguration;
import io.fluo.api.data.Bytes;
import io.fluo.api.data.Column;
import io.fluo.api.data.Span;
import io.fluo.api.iterator.ColumnIterator;
import io.fluo.api.iterator.RowIterator;
import io.fluo.api.types.TypedTransactionBase;
import io.fluo.commoncrawl.core.ColumnConstants;
import io.fluo.commoncrawl.data.util.FluoConstants;
import io.fluo.commoncrawl.data.util.Link;
import io.fluo.commoncrawl.data.util.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageLoader implements Loader {

  private static final Logger log = LoggerFactory.getLogger(PageLoader.class);

  private final Page page;

  public PageLoader(Page page) {
    this.page = page;
  }

  @Override
  public void load(TransactionBase tx, Context context) throws Exception {
    if (page.isEmpty() || page.getExternalLinks().isEmpty()) {
      return;
    }

    Bytes row = Bytes.of("p:"+page.getLink().getUri());

    TypedTransactionBase ttx = FluoConstants.TYPEL.wrap(tx);
    ScannerConfiguration scanConf = new ScannerConfiguration();
    scanConf.setSpan(Span.exact(row, new Column(ColumnConstants.OUTLINKS)));

    Set<Link> prevLinks = new HashSet<>();
    RowIterator rowIter = ttx.get(scanConf);
    if (rowIter.hasNext()) {
      Map.Entry<Bytes, ColumnIterator> rowEntry = rowIter.next();
      ColumnIterator colIter = rowEntry.getValue();
      while (colIter.hasNext()) {
        Map.Entry<Column, Bytes> colEntry = colIter.next();
        Link lnk = Link.fromUrl(colEntry.getKey().getQualifier().toString());
        log.info("Found link {}", lnk.getUrl());
        prevLinks.add(lnk);
      }
    }

    Set<Link> nextLinks = page.getExternalLinks();

    Sets.SetView<Link> addLinks = Sets.difference(nextLinks, prevLinks);
    for (Link link : addLinks) {
      ttx.set(row, new Column(ColumnConstants.OUTLINKS, link.getUri()), Bytes.of(link.getAnchorText()));
    }
    ttx.set(row, new Column(ColumnConstants.STATS, ColumnConstants.OUTLINKCOUNT),
            Bytes.of(Integer.toString(nextLinks.size())));

    Sets.SetView<Link> delLinks = Sets.difference(prevLinks, nextLinks);
    for (Link link : delLinks) {
      ttx.delete(row, new Column(ColumnConstants.OUTLINKS, link.getUri()));
    }
  }
}
